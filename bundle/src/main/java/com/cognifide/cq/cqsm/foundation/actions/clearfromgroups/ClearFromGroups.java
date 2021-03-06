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
package com.cognifide.cq.cqsm.foundation.actions.clearfromgroups;

import com.cognifide.cq.cqsm.api.actions.Action;
import com.cognifide.cq.cqsm.api.actions.ActionResult;
import com.cognifide.cq.cqsm.api.executors.Context;

public class ClearFromGroups implements Action {

	private ClearFromGroupOperationTypes operationTypes;

	public ClearFromGroups(ClearFromGroupOperationTypes operationType) {
		this.operationTypes = operationType;
	}

	@Override
	public ActionResult simulate(Context context) {
		ClearFromGroupDetacher detacher = new ClearFromGroupDetacher(context, true);
		return operationTypes.process(detacher);
	}

	@Override
	public ActionResult execute(Context context) {
		ClearFromGroupDetacher detacher = new ClearFromGroupDetacher(context, false);
		return operationTypes.process(detacher);
	}

	@Override
	public boolean isGeneric() {
		return false;
	}

}
