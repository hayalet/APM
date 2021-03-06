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
package com.cognifide.cq.cqsm.foundation.actions.removeuser;

import com.cognifide.cq.cqsm.api.actions.Action;
import com.cognifide.cq.cqsm.api.actions.BasicActionMapper;
import com.cognifide.cq.cqsm.api.actions.annotations.Mapping;
import com.cognifide.cq.cqsm.api.exceptions.ActionCreationException;

import java.util.Collections;
import java.util.List;

public final class RemoveUserMapper extends BasicActionMapper {

	public static final String REFERENCE = "Remove specified users.\n"
			+ "Removed user are no longer listed as any group members.\n"
			+ "Note that no permissions for removed users are cleaned, so after creating a new user with the same id"
			+ " - it will automatically gain those permissions.";

	@Mapping(
			value = {"REMOVE" + DASH + "USER" + SPACE + STRING},
			args = {"userId"},
			reference = REFERENCE
	)
	public Action mapAction(final String id) throws ActionCreationException {
		return mapAction(Collections.singletonList(id));
	}

	@Mapping(
			value = {"REMOVE" + DASH + "USER" + SPACE + LIST},
			args = {"userIds"},
			reference = REFERENCE
	)
	public Action mapAction(final List<String> ids) throws ActionCreationException {
		return new RemoveUser(ids);
	}

}
