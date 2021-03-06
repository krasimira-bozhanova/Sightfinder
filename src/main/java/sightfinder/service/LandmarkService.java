package sightfinder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sightfinder.dao.LandmarkDAO;
import sightfinder.model.Landmark;
import sightfinder.model.LandmarkType;
import sightfinder.util.Constants;
import sightfinder.util.Source;

@Service
@Transactional
public class LandmarkService {
	
	public static String[] meaninglessDescriptions = {
		"Описанието за този обект се подготвя и предстои да се публикува.",
		" Още информация",
		"Описанието за тази пещера предстои да бъде подготвено."
	};

	@Autowired
	private LandmarkDAO landmarkDAO;

	public Landmark save(Landmark landmark) {
		return landmarkDAO.save(landmark);
	}

	public Iterable<Landmark> saveLandmarks(Iterable<Landmark> landmarks) { 
		return landmarkDAO.save(landmarks); 
	}

	public Iterable<Landmark> getLandmarks() {
		return landmarkDAO.findAll();
	}

	public Landmark findLandmarkById(Long id) {
		return landmarkDAO.findOne(id);
	}

	public Iterable<Landmark> findLandmarksBySource(Source source) {
		return landmarkDAO.findBySource(source);
	}

	public Iterable<Landmark> getLandmarksWithType(LandmarkType type) { return landmarkDAO.findByLandmarkType(type); }

	public List<Landmark> findNearestLandmarks(Double latitude, Double longitude, Long maxDistance) {
		Double difference = 180 * maxDistance / (Math.PI * Constants.RADIUS_OF_EARTH);
		List<Landmark> landmarksWithNearestCoordinates = landmarkDAO.findByCoordinateRange(latitude - difference,
				latitude + difference, longitude - difference, longitude + difference);
		List<Landmark> nearestLandmarks = new ArrayList<Landmark>();

		for (Landmark landmark : landmarksWithNearestCoordinates) {
			Double distance = getDistance(latitude, longitude, landmark.getLatitude(), landmark.getLongitude());

			if (distance <= maxDistance) {
				landmark.setDistance(distance);
				nearestLandmarks.add(landmark);
			}
		}

		nearestLandmarks.sort(Landmark.LandmarkDistanceComparator);

		return nearestLandmarks;
	}

	public Iterable<Landmark> removeUselessDescription(Iterable<Long> documentIDs) {
		Iterable<Landmark> landmarks;
		if (documentIDs == null || !documentIDs.iterator().hasNext()) {
			landmarks = landmarkDAO.findAll();
		} else {
			List<Landmark> landmarksList = new ArrayList<Landmark>();
			for (Long documentID: documentIDs) {
				Landmark landmark = landmarkDAO.findOne(documentID);
				landmark.setDescription(fixDescription(landmark.getDescription()));
				landmarksList.add(landmark);
			}
			landmarks = landmarksList;
		}
		
		for (Landmark landmark: landmarks) {
			landmark.setDescription(fixDescription(landmark.getDescription()));
		}
		return landmarks;
	}

	private String fixDescription(String description) {
		for (String meaninglessDescription: meaninglessDescriptions) {
			description = description.replaceAll(meaninglessDescription, "");
		}
		return description;
			
	}

	private Double getDistance(Double latitude1, Double longitude1, Double latitude2, Double longitude2) {
		Double fi1 = Math.toRadians(latitude1);
		Double fi2 = Math.toRadians(latitude2);
		Double dfi = Math.toRadians(latitude2 - latitude1);
		Double dlambda = Math.toRadians(longitude2 - longitude1);
		Double a = Math.sin(dfi / 2) * Math.sin(dfi / 2) + Math.cos(fi1) * Math.cos(fi2) * Math.sin(dlambda / 2)
				* Math.sin(dlambda / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return Constants.RADIUS_OF_EARTH * c;
	}
}
