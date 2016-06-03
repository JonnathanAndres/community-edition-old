/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.repo.invitation.site;

import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarAcceptUrl;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarInviteTicket;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarInviteeGenPassword;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarInviteeUserName;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarInviterUserName;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarRejectUrl;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarResourceName;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarRole;
import static org.alfresco.repo.invitation.WorkflowModelNominatedInvitation.wfVarServerPath;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.invitation.activiti.SendNominatedInviteDelegate;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.cmr.invitation.InvitationException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ModelUtil;
import org.springframework.extensions.surf.util.ParameterCheck;
import org.springframework.extensions.surf.util.URLEncoder;

/**
 * This class is responsible for sending email invitations, allowing nominated
 * user's to join a Site.
 * 
 * @author Nick Smith
 */
public class InviteSender
{
    public static final String WF_INSTANCE_ID = "wf_instanceId";
    public static final String WF_PACKAGE = "wf_package";
    public static final String SITE_LEAVE_HASH = "#leavesite";
    private static final String SITE_DASHBOARD_ENDPOINT_PATTERN =  "/page/site/{0}/dashboard";

    private static final List<String> expectedProperties = Arrays.asList(wfVarInviteeUserName,//
                wfVarResourceName,//
                wfVarInviterUserName,//
                wfVarInviteeUserName,//
                wfVarRole,//
                wfVarInviteeGenPassword,//
                wfVarResourceName,//
                wfVarInviteTicket,//
                wfVarServerPath,//
                wfVarAcceptUrl,//
                wfVarRejectUrl, WF_INSTANCE_ID,//
                WF_PACKAGE);

    private final ActionService actionService;
    private final NodeService nodeService;
    private final PersonService personService;
    private final SearchService searchService;
    private final SiteService siteService;
    private final Repository repository;
    private final MessageService messageService;
    private final FileFolderService fileFolderService;
//    private final SysAdminParams sysAdminParams;
    private final RepoAdminService repoAdminService;
    private final NamespaceService namespaceService;
    
    public InviteSender(ServiceRegistry services, Repository repository, MessageService messageService)
    {
        this.actionService = services.getActionService();
        this.nodeService = services.getNodeService();
        this.personService = services.getPersonService();
        this.searchService = services.getSearchService();
        this.siteService = services.getSiteService();
        this.fileFolderService = services.getFileFolderService();
//        this.sysAdminParams = services.getSysAdminParams();
        this.repoAdminService = services.getRepoAdminService();
        this.namespaceService = services.getNamespaceService();
        this.repository = repository;
        this.messageService = messageService;
    }

    /**
     * Implemented for backwards compatibility
     * 
     * @param properties
     * @deprecated
     * @see {@link #sendMail(String, String, Map)}
     */
    public void sendMail(Map<String, String> properties)
    {
        sendMail(SendNominatedInviteDelegate.EMAIL_TEMPLATE_XPATH, SendNominatedInviteDelegate.EMAIL_SUBJECT_KEY, properties);
    }
    
    /**
     * Sends an invitation email.
     * 
     * @param emailTemplateXpath the XPath to the email template in the repository
     * @param emailSubjectKey the subject of the email
     * @param properties A Map containing the properties needed to send the email.
     */
    public void sendMail(String emailTemplateXpath, String emailSubjectKey, Map<String, String> properties)
    {
        checkProperties(properties);
        ParameterCheck.mandatory("Properties", properties);
        NodeRef inviter = personService.getPerson(properties.get(wfVarInviterUserName));
        String inviteeName = properties.get(wfVarInviteeUserName);
        NodeRef invitee = personService.getPerson(inviteeName);
        Action mail = actionService.createAction(MailActionExecuter.NAME);
        mail.setParameterValue(MailActionExecuter.PARAM_FROM, getEmail(inviter));
        mail.setParameterValue(MailActionExecuter.PARAM_TO, getEmail(invitee));
        mail.setParameterValue(MailActionExecuter.PARAM_SUBJECT, emailSubjectKey);
        mail.setParameterValue(MailActionExecuter.PARAM_SUBJECT_PARAMS, new Object[] {ModelUtil.getProductName(repoAdminService), getSiteName(properties)});
        mail.setParameterValue(MailActionExecuter.PARAM_TEMPLATE, getEmailTemplateNodeRef(emailTemplateXpath));
        mail.setParameterValue(MailActionExecuter.PARAM_TEMPLATE_MODEL, 
                (Serializable)buildMailTextModel(properties, inviter, invitee));
        mail.setParameterValue(MailActionExecuter.PARAM_IGNORE_SEND_FAILURE, true);
        actionService.executeAction(mail, getWorkflowPackage(properties));
    }

    /**
     * @param properties Map<String, String>
     */
    private void checkProperties(Map<String, String> properties)
    {
        Set<String> keys = properties.keySet();
        if (!keys.containsAll(expectedProperties))
        {
            LinkedList<String> missingProperties = new LinkedList<String>(expectedProperties);
            missingProperties.removeAll(keys);
            throw new InvitationException("The following mandatory properties are missing:\n" + missingProperties);
        }
    }

    private Map<String, Serializable> buildMailTextModel(Map<String, String> properties, NodeRef inviter, NodeRef invitee)
    {
        // Set the core model parts
        // Note - the user part is skipped, as that's implied via the run-as
        Map<String, Serializable> model = new HashMap<String, Serializable>();
        model.put(TemplateService.KEY_COMPANY_HOME, repository.getCompanyHome());
        model.put(TemplateService.KEY_USER_HOME, repository.getUserHome(repository.getPerson()));
        model.put(TemplateService.KEY_PRODUCT_NAME, ModelUtil.getProductName(repoAdminService));

        // Build up the args for rendering inside the template
        Map<String, String> args = buildArgs(properties, inviter, invitee);
        model.put("args", (Serializable)args);
        
        // All done
        return model;
    }

    private Map<String, String> buildArgs(Map<String, String> properties, NodeRef inviter, NodeRef invitee)
    {
        String params = buildUrlParamString(properties);
        String acceptLink = makeLink(properties.get(wfVarServerPath), properties.get(wfVarAcceptUrl), params, null);
        String rejectLink = makeLink(properties.get(wfVarServerPath), properties.get(wfVarRejectUrl), params, null);
        
        String siteDashboardEndpoint = getSiteDashboardEndpoint(properties);
        String siteDashboardLink = makeLink(properties.get(wfVarServerPath), 
                siteDashboardEndpoint, null, null);
        String siteLeaveLink = makeLink(properties.get(wfVarServerPath), 
                siteDashboardEndpoint, null, SITE_LEAVE_HASH);
        
        Map<String, String> args = new HashMap<String, String>();
        args.put("inviteePersonRef", invitee.toString());
        args.put("inviterPersonRef", inviter.toString());
        args.put("siteName", getSiteName(properties));
        args.put("inviteeSiteRole", getRoleName(properties));
        args.put("inviteeUserName", properties.get(wfVarInviteeUserName));
        args.put("inviteeGenPassword", properties.get(wfVarInviteeGenPassword));
        args.put("acceptLink", acceptLink);
        args.put("rejectLink", rejectLink);
        args.put("siteDashboardLink", siteDashboardLink);
        args.put("siteLeaveLink", siteLeaveLink);
        return args;
    }

    protected String makeLink(String location, String endpoint, String queryParams, String hashParam)
    {
        location = location.endsWith("/") ? location : location + "/";
        endpoint = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
        if (queryParams != null)
        {
            queryParams = queryParams.startsWith("?") ? queryParams : "?" + queryParams;
        }
        else
        {
            queryParams = "";
        }
        if (hashParam != null)
        {
            hashParam = hashParam.startsWith("#") ? hashParam : "#" + hashParam;
        }
        else
        {
            hashParam = "";
        }
        return location + endpoint + queryParams + hashParam;
    }

    private String getRoleName(Map<String, String> properties) 
    {
        String roleName = properties.get(wfVarRole);
        String role = messageService.getMessage("invitation.invitesender.email.role." + roleName);
        if (role == null)
        {
            role = roleName;
        }
        return role;
    }

    private String getEmail(NodeRef person)
    {
        return (String) nodeService.getProperty(person, ContentModel.PROP_EMAIL);
    }

    private NodeRef getWorkflowPackage(Map<String, String> properties)
    {
        String packageRef = properties.get(WF_PACKAGE);
        return new NodeRef(packageRef);
    }

    private NodeRef getEmailTemplateNodeRef(String emailTemplateXPath)
    {
        List<NodeRef> nodeRefs = searchService.selectNodes(repository.getRootHome(), 
                    emailTemplateXPath, null, 
                    this.namespaceService, false);
        
        if (nodeRefs.size() == 1) 
        {
            // Now localise this
            NodeRef base = nodeRefs.get(0);
            NodeRef local = fileFolderService.getLocalizedSibling(base);
            return local;
        }
        else
        {
            throw new InvitationException("Cannot find the email template!");
        }
    }

    private String buildUrlParamString(Map<String, String> properties)
    {
        StringBuilder params = new StringBuilder("?inviteId=");
        params.append(properties.get(WF_INSTANCE_ID));
        params.append("&inviteeUserName=");
        params.append(URLEncoder.encode(properties.get(wfVarInviteeUserName)));
        params.append("&siteShortName=");
        params.append(properties.get(wfVarResourceName));
        params.append("&inviteTicket=");
        params.append(properties.get(wfVarInviteTicket));
        return params.toString();
    }

    private String getSiteName(Map<String, String> properties)
    {
        String siteFullName = properties.get(wfVarResourceName);
        SiteInfo site = siteService.getSite(siteFullName);
        if (site == null)
            throw new InvitationException("The site " + siteFullName + " could not be found.");

        String siteName = site.getShortName();
        String siteTitle = site.getTitle();
        if (siteTitle != null && siteTitle.length() > 0)
        {
            siteName = siteTitle;
        }
        return siteName;
    }
    
    private String getSiteDashboardEndpoint(Map<String, String> properties)
    {
        String siteName = properties.get(wfVarResourceName);
        return MessageFormat.format(SITE_DASHBOARD_ENDPOINT_PATTERN, siteName);
    }
}
