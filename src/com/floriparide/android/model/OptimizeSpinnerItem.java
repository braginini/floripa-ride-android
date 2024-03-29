/*
 * Copyright 2013 Mikhail Bragin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.floriparide.android.model;

import org.opentripplanner.routing.core.OptimizeType;

public class OptimizeSpinnerItem {

	private String displayName;
	private OptimizeType optimizeType;
	
	public OptimizeSpinnerItem () {
		
	}
	
	public OptimizeSpinnerItem(String displayName, OptimizeType optimizeType){
		this.displayName = displayName;
		this.optimizeType = optimizeType;
	}
	
	public String toString(){
		return displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public OptimizeType getOptimizeType() {
		return optimizeType;
	}

	public void setOptimizeType(OptimizeType optimizeType) {
		this.optimizeType = optimizeType;
	}
}
