/*
 * Copyright 2013 Mikhail Bragin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.floriparide.android.util;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.api.model.AgencyAndId;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.patch.Alerts;
import org.opentripplanner.v092snapshot.api.model.AbsoluteDirection;
import org.opentripplanner.v092snapshot.api.model.Leg;
import org.opentripplanner.v092snapshot.api.model.Place;
import org.opentripplanner.v092snapshot.api.model.WalkStep;

import android.content.Context;
import android.content.res.Resources;
import com.floriparide.android.OTPApp;
import com.floriparide.android.R;
import com.floriparide.android.model.Direction;

/**
 * @author Khoa Tran
 *
 */

public class ItineraryDecrypt {
	private List<Leg> legs = new ArrayList<Leg>();

	private ArrayList<Direction> directions = new ArrayList<Direction>();

	private double totalDistance = 0;

	private double totalTimeTraveled = 0;
	
	private Context applicationContext;
	
	int agencyTimeZoneOffset = 0;

	public ItineraryDecrypt(List<Leg> legs, Context applicationContext){
		this.legs.addAll(legs);
		this.applicationContext = applicationContext;
		
		convertToDirectionList();
	}

	/**
	 * @return the directions
	 */
	public ArrayList<Direction> getDirections() {
		return directions;
	}

	/**
	 * @param directions the directions to set
	 */
	public void setDirections(ArrayList<Direction> directions) {
		this.directions = directions;
	}

	public void addDirection(Direction dir){
		if(directions==null){
			directions = new ArrayList<Direction>();
		}

		directions.add(dir);
	}

	private void convertToDirectionList(){
		int index = 0;
		for(Leg leg: legs){
			index++;
			setTotalDistance(getTotalDistance() + leg.distance);

			TraverseMode traverseMode = TraverseMode.valueOf((String) leg.mode);
			if(traverseMode.isOnStreetNonTransit()){
				Direction dir = decryptNonTransit(leg);
				if(dir == null){
					continue;
				}
				dir.setDirectionIndex(index);
				addDirection(dir);
			} else{
				ArrayList<Direction> directions = decryptTransit(leg);
				if(directions == null){
					continue;
				}

				if(directions.get(0)!=null){
					directions.get(0).setDirectionIndex(index);
					addDirection(directions.get(0));
				}

				if(directions.get(1)!=null){
					if (directions.get(0)!=null){
						index++;
					}
					directions.get(1).setDirectionIndex(index);
					addDirection(directions.get(1));
				}
			}

			//			directionText+=leg.mode+"\n";
		}
	}

	private Direction decryptNonTransit(Leg leg){
		Direction direction = new Direction();

		//		http://opentripplanner.usf.edu/opentripplanner-api-webapp/ws/plan?optimize=QUICK&time=09:24pm&arriveBy=false&wheelchair=false&maxWalkDistance=7600.0&fromPlace=28.033389%2C+-82.521034&toPlace=28.064709%2C+-82.471618&date=03/07/12&mode=WALK,TRAM,SUBWAY,RAIL,BUS,FERRY,CABLE_CAR,GONDOLA,FUNICULAR,TRANSIT,TRAINISH,BUSISH
		
		if (leg.getAgencyTimeZoneOffset() != 0){
			this.agencyTimeZoneOffset = leg.getAgencyTimeZoneOffset();
		}
		
		// Get appropriate action and icon
		// Get appropriate action and icon
		String action = applicationContext.getResources().getString(R.string.mode_walk_action);
		TraverseMode mode = TraverseMode.valueOf((String) leg.mode);
		int icon = getModeIcon(new TraverseModeSet(mode));
		if(mode.compareTo(TraverseMode.BICYCLE)==0){
			action = applicationContext.getResources().getString(R.string.mode_bicycle_action);
		} else if(mode.compareTo(TraverseMode.CAR)==0){
			action = applicationContext.getResources().getString(R.string.mode_car_action);
		}


		direction.setIcon(icon);

		//		Main direction
		Place fromPlace = leg.from;
		Place toPlace = leg.to;
		String mainDirectionText = action;
		mainDirectionText += fromPlace.name==null ? "" : " " + applicationContext.getResources().getString(R.string.step_by_step_from) + " " + getLocalizedStreetName(fromPlace.name, applicationContext.getResources());
		mainDirectionText += toPlace.name==null ? "" : " " + applicationContext.getResources().getString(R.string.step_by_step_to) + " " + getLocalizedStreetName(toPlace.name, applicationContext.getResources());
		mainDirectionText += toPlace.stopId==null ? "" : " (" + toPlace.stopId.getAgencyId() + " " + toPlace.stopId.getId() + ")";
//		double duration = DateTimeConversion.getDuration(leg.startTime, leg.endTime);
		double totalDistance = leg.distance;
		mainDirectionText += "\n[" + String.format(OTPApp.FORMAT_DISTANCE_METERS_FULL, totalDistance) + applicationContext.getResources().getString(R.string.distance_unit) +" ]";// Double.toString(duration);

		direction.setDirectionText(mainDirectionText);

		//		Sub-direction
		List<WalkStep> walkSteps = leg.getWalkSteps();

		if(walkSteps==null) return direction;

		ArrayList<Direction> subDirections = new ArrayList<Direction>(walkSteps.size());

		for(WalkStep step: walkSteps){
			Direction dir = new Direction();
			String subDirectionText = "";

			double distance = step.distance;
			// Distance traveled [distance]
	//		distance = Double.valueOf(twoDForm.format(distance)); -->VREIXO
			dir.setDistanceTraveled(distance);

			RelativeDirection relativeDir = step.relativeDirection;
			String relativeDirString = getLocalizedRelativeDir(relativeDir, applicationContext.getResources());
			String streetName = step.streetName;
			AbsoluteDirection absoluteDir = step.absoluteDirection;
			String absoluteDirString = getLocalizedAbsoluteDir(absoluteDir, applicationContext.getResources());
			String exit = step.exit;
			boolean isStayOn = (step.stayOn==null ? false : step.stayOn);
			boolean isBogusName = (step.bogusName==null ? false : step.bogusName);
			double lon = step.lon;
			double lat = step.lat;
			//Elevation[] elevation = step.getElevation();  //Removed elevation for now, since we're not doing anything with it and it causes version issues between OTP server APIs v0.9.1-SNAPSHOT and v0.9.2-SNAPSHOT
			List<Alerts> alert = step.alerts;

			// Walk East
			if(relativeDir==null){
				subDirectionText += action + " " + applicationContext.getResources().getString(R.string.step_by_step_heading) + " ";
				subDirectionText += absoluteDirString + " ";
			}
			// (Turn left)/(Continue) 
			else {
				if(!isStayOn) {
					RelativeDirection rDir = RelativeDirection.valueOf(relativeDir.name());

					// Do not need TURN Continue
					if( rDir.compareTo(RelativeDirection.CONTINUE) != 0 &&
							rDir.compareTo(RelativeDirection.CIRCLE_CLOCKWISE) !=0 &&
							rDir.compareTo(RelativeDirection.CIRCLE_COUNTERCLOCKWISE) != 0){
						subDirectionText += applicationContext.getResources().getString(R.string.step_by_step_turn) + " ";
					}

					subDirectionText += relativeDirString + " ";
				} else {
					subDirectionText += relativeDirString + " ";
				}
			}

			// (on ABC)
			//			if(!isBogusName) {
			//				subDirectionText += "on "+ streetName + " ";
			//			}
			
			subDirectionText += applicationContext.getResources().getString(R.string.step_by_step_connector_street_name) + " "+ getLocalizedStreetName(streetName, applicationContext.getResources()) + " ";
			
			subDirectionText += "\n[" + String.format(OTPApp.FORMAT_DISTANCE_METERS_FULL, distance) + applicationContext.getResources().getString(R.string.distance_unit) + " ]";

			dir.setDirectionText(subDirectionText);

			dir.setIcon(icon);

			// Add new sub-direction
			subDirections.add(dir);
		}

		direction.setSubDirections(subDirections);

		return direction;
	}
	
	// Dirty fix to avoid the presence of names for unnamed streets (as road, track, etc.) for other languages than English
	public static String getLocalizedStreetName(String streetName, Resources resources){
		if (streetName != null){
			if (streetName.equals("bike path")){
				return resources.getString(R.string.street_type_bike_path);
	        }
	        else if (streetName.equals("open area")){
				return resources.getString(R.string.street_type_open_area);
	        }
	        else if (streetName.equals("path")){
				return resources.getString(R.string.street_type_path);
	        }
	        else if (streetName.equals("bridleway")){
				return resources.getString(R.string.street_type_bridleway);
	        }
	        else if (streetName.equals("footpath")){
				return resources.getString(R.string.street_type_footpath);
	        }
	        else if (streetName.equals("platform")){
				return resources.getString(R.string.street_type_platform);
	        }
	        else if (streetName.equals("footbridge")){
				return resources.getString(R.string.street_type_footbridge);
	        }
	        else if (streetName.equals("underpass")){
				return resources.getString(R.string.street_type_underpass);
	        }
	        else if (streetName.equals("road")){
				return resources.getString(R.string.street_type_road);
	        }
	        else if (streetName.equals("ramp")){
				return resources.getString(R.string.street_type_ramp);
	        }
	        else if (streetName.equals("link")){
				return resources.getString(R.string.street_type_link);
	        }
	        else if (streetName.equals("service road")){
				return resources.getString(R.string.street_type_service_road);
	        }
	        else if (streetName.equals("alley")){
				return resources.getString(R.string.street_type_alley);
	        }
	        else if (streetName.equals("parking aisle")){
				return resources.getString(R.string.street_type_parking_aisle);
	        }
	        else if (streetName.equals("byway")){
				return resources.getString(R.string.street_type_byway);
	        }
	        else if (streetName.equals("track")){
				return resources.getString(R.string.street_type_track);
	        }
	        else if (streetName.equals("sidewalk")){
				return resources.getString(R.string.street_type_sidewalk);
	        }
	        else if (streetName.equals("steps")){
				return resources.getString(R.string.street_type_steps);
	        }
	        else{
	        	return streetName;
	        }
		}
        return null;
	}
	
	public static String getLocalizedRelativeDir(RelativeDirection relDir, Resources resources){
		if (relDir != null){
			if (relDir.equals(RelativeDirection.CIRCLE_CLOCKWISE)){
				return resources.getString(R.string.dir_relative_circle_clockwise);
			}
			else if (relDir.equals(RelativeDirection.CIRCLE_COUNTERCLOCKWISE)){
				return resources.getString(R.string.dir_relative_circle_counterclockwise);
			}
			else if (relDir.equals(RelativeDirection.CONTINUE)){
				return resources.getString(R.string.dir_relative_continue);
			}
			else if (relDir.equals(RelativeDirection.DEPART)){
				return resources.getString(R.string.dir_relative_depart);
			}
			else if (relDir.equals(RelativeDirection.ELEVATOR)){
				return resources.getString(R.string.dir_relative_elevator);
			}
			else if (relDir.equals(RelativeDirection.HARD_LEFT)){
				return resources.getString(R.string.dir_relative_hard_left);
			}
			else if (relDir.equals(RelativeDirection.HARD_RIGHT)){
				return resources.getString(R.string.dir_relative_hard_right);
			}
			else if (relDir.equals(RelativeDirection.LEFT)){
				return resources.getString(R.string.dir_relative_left);
			}
			else if (relDir.equals(RelativeDirection.RIGHT)){
				return resources.getString(R.string.dir_relative_right);
			}
			else if (relDir.equals(RelativeDirection.SLIGHTLY_LEFT)){
				return resources.getString(R.string.dir_relative_slightly_left);
			}
			else if (relDir.equals(RelativeDirection.SLIGHTLY_RIGHT)){
				return resources.getString(R.string.dir_relative_slightly_right);
			}
			else if (relDir.equals(RelativeDirection.UTURN_LEFT)){
				return resources.getString(R.string.dir_relative_uturn_left);
			}
			else if (relDir.equals(RelativeDirection.UTURN_RIGHT)){
				return resources.getString(R.string.dir_relative_uturn_right);
			}
		}
		return null;
	}
	
	public static String getLocalizedAbsoluteDir(AbsoluteDirection absDir, Resources resources){
		if (absDir != null){
			if (absDir.equals(AbsoluteDirection.EAST)){
				return resources.getString(R.string.dir_absolute_east);
			}
			else if (absDir.equals(AbsoluteDirection.NORTH)){
				return resources.getString(R.string.dir_absolute_north);
			}
			else if (absDir.equals(AbsoluteDirection.NORTHEAST)){
				return resources.getString(R.string.dir_absolute_northeast);
			}
			else if (absDir.equals(AbsoluteDirection.NORTHWEST)){
				return resources.getString(R.string.dir_absolute_northwest);
			}
			else if (absDir.equals(AbsoluteDirection.SOUTH)){
				return resources.getString(R.string.dir_absolute_south);
			}
			else if (absDir.equals(AbsoluteDirection.SOUTHEAST)){
				return resources.getString(R.string.dir_absolute_southeast);
			}
			else if (absDir.equals(AbsoluteDirection.SOUTHWEST)){
				return resources.getString(R.string.dir_absolute_southwest);
			}
			else if (absDir.equals(AbsoluteDirection.WEST)){
				return resources.getString(R.string.dir_absolute_west);
			}
		}
		return null;
	}

	private ArrayList<Direction> decryptTransit(Leg leg){
		ArrayList<Direction> directions = new ArrayList<Direction>(2);
		Direction onDirection = new Direction();
		Direction offDirection = new Direction(); 
		
		if (leg.getAgencyTimeZoneOffset() != 0){
			this.agencyTimeZoneOffset = leg.getAgencyTimeZoneOffset();
		}

		//		set icon
		TraverseMode mode = TraverseMode.valueOf((String) leg.mode);
		int icon = getModeIcon(new TraverseModeSet(mode));

		onDirection.setIcon(icon);

		//		set direction text
		String onDirectionText = "";
		String offDirectionText = "";

		String route = leg.route;
		String agencyName = leg.agencyName;
		String agencyUrl = leg.agencyId;
		String routeColor = leg.routeColor;
		String routeTextColor = leg.routeTextColor;
		boolean isInterlineWithPreviousLeg = (leg.interlineWithPreviousLeg==null?false:leg.interlineWithPreviousLeg);
		String tripShortName = leg.tripShortName;
		String headsign = leg.headsign;
		String agencyId = leg.agencyId;
		String routeShortName = leg.routeShortName;
		String routeLongName = leg.routeLongName;
		String boardRule = leg.boardRule;
		String alignRule = leg.alightRule;
		String startTime = leg.startTime;

		ArrayList<Place> stopsInBetween = new ArrayList<Place>();
		if(leg.getStop()!=null)
			stopsInBetween.addAll(leg.getStop());

		double distance = leg.distance;
		Place from = leg.from;
		AgencyAndId agencyAndIdFrom = from.stopId;
		Place to = leg.to;
		AgencyAndId agencyAndIdTo = to.stopId;
		long duration = leg.duration;

		// Get on HART BUS 6
		String serviceName = agencyName;
		if(serviceName==null)
			serviceName = "";

		offDirectionText += applicationContext.getResources().getString(R.string.step_by_step_transit_get_off) + " " + serviceName + " " + mode + " " + route + "\n";
		offDirectionText += applicationContext.getResources().getString(R.string.step_by_step_transit_connector_stop_name) + " " + to.name + " (" + agencyAndIdTo.getAgencyId() + " " + agencyAndIdTo.getId() + ")";
		offDirection.setDirectionText(offDirectionText);
		offDirection.setIcon(icon);

		// Only onDirection has subdirection (list of stops in between)
		
		onDirectionText += applicationContext.getResources().getString(R.string.step_by_step_transit_get_on) + " " + serviceName + " " + mode + " " + route + DateTimeConversion.getTimeWithContext(applicationContext, agencyTimeZoneOffset, Long.parseLong(leg.getStartTime()), true) + "\n";
		onDirectionText += applicationContext.getResources().getString(R.string.step_by_step_transit_connector_stop_name) + " " + from.name + " (" + agencyAndIdFrom.getAgencyId() + " " + agencyAndIdFrom.getId() + ")\n";
		onDirectionText += stopsInBetween.size() + " " + applicationContext.getResources().getString(R.string.step_by_step_transit_stops_in_between);
		onDirection.setDirectionText(onDirectionText);
		onDirection.setIcon(icon);

		// sub-direction
		ArrayList<Direction> subDirections = new ArrayList<Direction>();
		for(int i=0; i<stopsInBetween.size(); i++){
			Direction subDirection = new Direction();

			Place stop = stopsInBetween.get(i);
			AgencyAndId agencyAndIdStop = stop.stopId;
			String subDirectionText = 	Integer.toString(i) + ". " +stop.name + " (" + 
					agencyAndIdStop.getAgencyId() + " " + 
					agencyAndIdStop.getId() + ")";

			subDirection.setDirectionText(subDirectionText);
			subDirection.setIcon(icon);

			subDirections.add(subDirection);
		}
		onDirection.setSubDirections(subDirections);

		// Distance traveled [distance]
	//	distance = Double.valueOf(twoDForm.format(distance)); -->VREIXO
		onDirection.setDistanceTraveled(distance);

		directions.add(onDirection);
		directions.add(offDirection);

		return directions;
	}
	
	public static int getModeIcon(TraverseModeSet mode){
		if (mode.contains(TraverseMode.FERRY) && 
				mode.contains(TraverseMode.BUSISH) &&
					mode.contains(TraverseMode.TRAINISH)){
			return R.drawable.mode_transit;
		}
		else if(mode.contains(TraverseMode.BUSISH)){
			return R.drawable.mode_bus;
		}
		else if(mode.contains(TraverseMode.TRAINISH)){
			return R.drawable.mode_train;
		} else if(mode.contains(TraverseMode.FERRY)){
			return R.drawable.mode_ferry;
		} else if(mode.contains(TraverseMode.GONDOLA)){
			return R.drawable.mode_ferry;
		} else if(mode.contains(TraverseMode.SUBWAY)){
			return R.drawable.mode_metro;
		} else if(mode.contains(TraverseMode.TRAM)){
			return R.drawable.mode_train;
		}else if(mode.contains(TraverseMode.WALK)){
			return R.drawable.mode_walk;
		}else if(mode.contains(TraverseMode.BICYCLE)){
			return R.drawable.mode_bike;
		}else if(mode.contains(TraverseMode.CAR)){
			return R.drawable.mode_car;
		}else {
			return R.drawable.icon;
		}
	}

	/**
	 * @return the totalDistance
	 */
	public double getTotalDistance() {
	//	totalDistance = Double.valueOf(twoDForm.format(totalDistance));-->VREIXO
		return totalDistance;
	}

	/**
	 * @param totalDistance the totalDistance to set
	 */
	public void setTotalDistance(double totalDistance) {
		this.totalDistance = totalDistance;
	}

	/**
	 * @return the totalTimeTraveled
	 */
	public double getTotalTimeTraveled() {
		if(legs.isEmpty()) return 0;

		Leg legStart = legs.get(0);
		String startTimeText = legStart.startTime;
		Leg legEnd = legs.get(legs.size()-1);
		String endTimeText = legEnd.endTime;

		totalTimeTraveled = DateTimeConversion.getDuration(startTimeText, endTimeText);

		return totalTimeTraveled;
	}

}
