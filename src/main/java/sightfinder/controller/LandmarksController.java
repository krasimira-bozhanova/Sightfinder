package sightfinder.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sightfinder.model.Landmark;
import sightfinder.service.DBPediaService;
import sightfinder.service.FacebookService;
import sightfinder.service.IRService;
import sightfinder.service.LandmarkService;
import sightfinder.service.LandmarkTypeService;
import sightfinder.util.Constants;

/**
 * Created by krasimira on 31.01.16.
 */
@RestController
@RequestMapping("/landmarks")
public class LandmarksController {

	@Autowired
	FacebookService facebookService;

	@Autowired
	DBPediaService dbPediaService;

	@Autowired
	LandmarkService landmarkService;

	@Autowired
	IRService informationRetrievalService;

	@Autowired
	LandmarkTypeService landmarkTypeService;

	@CrossOrigin
	@RequestMapping(method = RequestMethod.GET)
	public Iterable<Landmark> getLandmarks(@RequestParam(required = false) Long typeId) {
        if (typeId == null) {
            return landmarkService.getLandmarks();
        }
        return landmarkService.getLandmarksWithType(landmarkTypeService.findLandmarkTypeById(typeId));
	}

	@RequestMapping(value = "/{id:.*}", method = RequestMethod.GET)
	public Landmark getLandmark(@PathVariable Long id) {
		return landmarkService.findLandmarkById(id);
	}

	@RequestMapping(method = RequestMethod.POST)
	public Iterable<Landmark> saveAllLandmarks(@RequestBody List<Landmark> landmarks) {
		return landmarkService.saveLandmarks(landmarks);
	}

	@RequestMapping(value = "/working-time", method = RequestMethod.POST)
	public List<Landmark> updateWorkingTime() {
		Iterable<Landmark> landmarks = landmarkService.getLandmarks();
		List<Landmark> updatedLanmarks = new ArrayList<Landmark>();
		for (Landmark landmark : landmarks) {
			if (landmark.getLandmarkType() != null && !landmark.getLandmarkType().getHasWorkingTime()) {
				continue;
			}

			try {
				List<String> externalLinks = dbPediaService.fetchExternalLinks(landmark);
				if (externalLinks != null) {
					for (String externalLink : externalLinks) {
						Landmark updatedLandmark = facebookService.updateWorkingTime(externalLink, landmark);
						if (updatedLandmark != null) {
							DateTime from = new DateTime(landmark.getWorkingTimeFrom());
							DateTime to = new DateTime(landmark.getWorkingTimeTo());
							System.out.printf("Found working time for landmark %s: from %s to %s",
									updatedLandmark.getName(), facebookService.timeFormat.print(from),
									facebookService.timeFormat.print(to));
							updatedLanmarks.add(updatedLandmark);
							landmarkService.save(updatedLandmark);
							break;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
		return updatedLanmarks;
	}

	@RequestMapping("/near/{latitude}/{longitude}/")
	public List<Landmark> findNearestLandmarks(@PathVariable Double latitude, @PathVariable Double longitude) {
		return landmarkService.findNearestLandmarks(latitude, longitude, Constants.MAX_DISTANCE);
	}
	
	@RequestMapping("/summary")
	public String summarize(@RequestParam ArrayList<Long> documentIDs) {
		Set<String> descriptions = new HashSet<String>();
		for (Long documentID: documentIDs) {
			descriptions.add(landmarkService.findLandmarkById(documentID).getDescription());
		}
		try {
			return informationRetrievalService.summarize(descriptions);
		} catch (Exception e) { 
			e.printStackTrace();
			return "";
		}
	}
	
	@RequestMapping(value = "/fix-descriptions")
	public Iterable<Landmark> getLandmarksWithoutUselessDescription(@RequestParam ArrayList<Long> documentIDs) {
		return landmarkService.removeUselessDescription(documentIDs);
	}
	
	@RequestMapping(value = "/fix-descriptions", method = RequestMethod.POST)
	public Iterable<Landmark> removeUselessDescription(@RequestParam ArrayList<Long> documentIDs) {
		Iterable<Landmark> landmarks = landmarkService.removeUselessDescription(documentIDs);
		return landmarkService.saveLandmarks(landmarks);
	}
}
