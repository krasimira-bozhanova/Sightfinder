package sightfinder.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sightfinder.dao.LandmarkDAO;
import sightfinder.model.Landmark;

@Service
@Transactional
public class LandmarkService {

    @Autowired
    private LandmarkDAO landmarkDAO;

    public Landmark save(Landmark landmark) {
    	return landmarkDAO.save(landmark);
    }
}
