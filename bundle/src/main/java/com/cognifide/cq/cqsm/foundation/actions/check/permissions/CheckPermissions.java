/*-
 * ========================LICENSE_START=================================
 * AEM Permission Management
 * %%
 * Copyright (C) 2013 Cognifide Limited
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package com.cognifide.cq.cqsm.foundation.actions.check.permissions;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.cognifide.cq.cqsm.api.actions.Action;
import com.cognifide.cq.cqsm.api.actions.ActionResult;
import com.cognifide.cq.cqsm.api.actions.interfaces.ResourceResolvable;
import com.cognifide.cq.cqsm.api.exceptions.ActionExecutionException;
import com.cognifide.cq.cqsm.api.executors.Context;
import com.cognifide.cq.cqsm.api.utils.AuthorizablesUtils;
import com.cognifide.cq.cqsm.core.actions.ActionUtils;
import com.cognifide.cq.cqsm.core.utils.MessagingUtils;
import com.day.cq.security.util.CqActions;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

public class CheckPermissions implements Action, ResourceResolvable {

	private final String path;

	private final String glob;

	private final List<String> permissions;

	private final boolean isAllow;

	private final String authorizableId;

	private ResourceResolver resourceResolver;

	public CheckPermissions(final String authorizableId, final String path, final String glob,
			final List<String> permissions, boolean isAllow) {
		this.authorizableId = authorizableId;
		this.path = path;
		this.glob = glob;
		this.permissions = permissions;
		this.isAllow = isAllow;
	}

	@Override
	public ActionResult simulate(Context context) {
		return process(context, false);
	}

	@Override
	public ActionResult execute(final Context context) {
		return process(context, true);
	}

	private ActionResult process(final Context context, boolean execute) {
		ActionResult actionResult = new ActionResult();
		try {
			final Authorizable authorizable = AuthorizablesUtils.getAuthorizable(context, authorizableId);

			final Set<Principal> authorizablesToCheck = getAuthorizablesToCheck(authorizable, context);

			final CqActions actions = new CqActions(context.getSession());

			final List<String> privilegesToCheck = preparePrivilegesToCheck();

			if (StringUtils.isBlank(glob)) {
				if (checkPermissionsForPath(authorizablesToCheck, actions, privilegesToCheck, path)) {
					logFailure(execute, actionResult, authorizable, path);
				} else {
					actionResult.logMessage(
							"All required privileges are set for " + authorizable.getID() + " on " + path);
				}
			} else {
				checkPermissionsForGlob(execute, actionResult, authorizable, authorizablesToCheck, actions,
						privilegesToCheck);
			}

		} catch (final PathNotFoundException e) {
			actionResult.logError("Path " + path + " not found");
		} catch (RepositoryException | ActionExecutionException | LoginException e) {
			actionResult.logError(MessagingUtils.createMessage(e));
		}
		return actionResult;
	}

	private void checkPermissionsForGlob(final boolean execute, final ActionResult actionResult,
			final Authorizable authorizable, final Set<Principal> authorizablesToCheck,
			final CqActions actions, final List<String> privilegesToCheck)
			throws RepositoryException, LoginException {
		final List<String> subpaths = getAllSubpaths(path);
		Pattern pattern = Pattern.compile(path + StringUtils.replace(glob, "*", ".*"));
		boolean foundMatch = false;
		boolean failed = false;
		for (String subpath : subpaths) {
			if (pattern.matcher(subpath).matches()) {
				foundMatch = true;
				failed = checkPermissionsForPath(authorizablesToCheck, actions, privilegesToCheck, subpath);
				if (failed) {
					logFailure(execute, actionResult, authorizable, subpath);
					break;
				}
			}
		}
		if (!foundMatch) {
			actionResult
					.logError("No match was found for " + authorizable.getID() + " for given glob " + glob);
			if (execute) {
				actionResult.logError(ActionUtils.ASSERTION_FAILED_MSG);
			}
		} else if (!failed) {
			actionResult.logMessage(
					"All required privileges are set for " + authorizable.getID() + " on " + path);
		}
	}

	private boolean checkPermissionsForPath(final Set<Principal> authorizablesToCheck,
			final CqActions actions, final List<String> privilegesToCheck, String subpath)
			throws RepositoryException {
		Collection<String> allowedActions = actions.getAllowedActions(subpath, authorizablesToCheck);
		final boolean containsAll = allowedActions.containsAll(privilegesToCheck);
		return (!containsAll && isAllow) || (containsAll && !isAllow);
	}

	private void logFailure(boolean execute, ActionResult actionResult, final Authorizable authorizable,
			String subpath) throws RepositoryException {
		actionResult.logError(
				"Not all required privileges are set for " + authorizable.getID() + " on " + subpath);
		if (execute) {
			actionResult.logError(ActionUtils.ASSERTION_FAILED_MSG);
		}
	}

	private List<String> getAllSubpaths(final String path) throws RepositoryException, LoginException {
		List<String> subpaths = new ArrayList<>();
		Resource resource = resourceResolver.getResource(path);
		Node node = resource.adaptTo(Node.class);

		subpaths.addAll(crawl(node));

		return subpaths;
	}

	private List<String> crawl(final Node node) throws RepositoryException {
		List<String> paths = new ArrayList<>();
		paths.add(node.getPath());
		for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
			paths.addAll(crawl(iter.nextNode()));
		}
		return paths;
	}

	private Set<Principal> getAuthorizablesToCheck(Authorizable authorizable, Context context)
			throws RepositoryException {
		Set<Principal> principals = new HashSet<>();
		Principal principal = authorizable.getPrincipal();
		principals.add(principal);

		for (PrincipalIterator it = (context.getSession()).getPrincipalManager()
				.getGroupMembership(principal); it.hasNext(); ) {
			principals.add(it.nextPrincipal());
		}

		return principals;
	}

	private List<String> preparePrivilegesToCheck() throws RepositoryException {
		return Lists.transform(permissions, new toLowerCase());
	}

	@Override
	public boolean isGeneric() {
		return true;
	}

	@Override
	public void setResourceResolver(ResourceResolver resolver) {
		this.resourceResolver = resolver;
	}

	@Override
	public void setResourceResolverFactory(ResourceResolverFactory factory) {
	}

	private static class toLowerCase implements Function<String, String> {
		@Override
		public String apply(String input) {
			return input.toLowerCase();
		}
	}
}
