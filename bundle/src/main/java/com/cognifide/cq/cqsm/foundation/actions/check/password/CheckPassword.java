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
package com.cognifide.cq.cqsm.foundation.actions.check.password;

import com.cognifide.cq.cqsm.api.actions.Action;
import com.cognifide.cq.cqsm.api.actions.ActionResult;
import com.cognifide.cq.cqsm.api.actions.interfaces.ResourceResolvable;
import com.cognifide.cq.cqsm.api.exceptions.ActionExecutionException;
import com.cognifide.cq.cqsm.api.executors.Context;
import com.cognifide.cq.cqsm.api.utils.AuthorizablesUtils;
import com.cognifide.cq.cqsm.core.actions.ActionUtils;
import com.cognifide.cq.cqsm.core.utils.MessagingUtils;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

public class CheckPassword implements Action, ResourceResolvable {

	private final String userPassword;

	private final String userId;

	private ResourceResolverFactory resolverFactory;

	public CheckPassword(final String userId, final String userPassword) {
		this.userId = userId;
		this.userPassword = userPassword;
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
			if (!tryGetUser(context, actionResult)) {
				return actionResult;
			}
			boolean loginSuccessful = checkLogin();
			if (!loginSuccessful) {
				actionResult.logError("Credentials for user " + userId + " seem invalid");
				if (execute) {
					actionResult.logError(ActionUtils.ASSERTION_FAILED_MSG);
				}
			}
		} catch (RepositoryException | ActionExecutionException e) {
			actionResult.logError(MessagingUtils.createMessage(e));
		}
		return actionResult;
	}

	private boolean tryGetUser(final Context context, final ActionResult actionResult)
			throws ActionExecutionException, RepositoryException {
		User user = AuthorizablesUtils.getUserIfExists(context, userId);
		if (user == null) {
			actionResult.logError(MessagingUtils.authorizableNotExists(userId));
			return false;
		}
		return true;
	}

	private boolean checkLogin() {
		Map<String, Object> authenticationInfo = new HashMap<>();
		authenticationInfo.put(ResourceResolverFactory.USER, userId);
		authenticationInfo.put(ResourceResolverFactory.PASSWORD, userPassword.toCharArray());
		boolean loginSuccessful = true;

		try {
			resolverFactory.getResourceResolver(authenticationInfo).close();
		} catch (LoginException e) {
			loginSuccessful = false;
		}
		return loginSuccessful;

	}

	@Override
	public boolean isGeneric() {
		return true;
	}

	@Override
	public void setResourceResolver(ResourceResolver resolver) {
	}

	@Override
	public void setResourceResolverFactory(ResourceResolverFactory factory) {
		this.resolverFactory = factory;
	}
}
