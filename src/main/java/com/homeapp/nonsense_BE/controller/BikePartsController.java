package com.homeapp.nonsense_BE.controller;

import com.homeapp.nonsense_BE.models.bike.BikeParts;
import com.homeapp.nonsense_BE.models.bike.FullBike;
import com.homeapp.nonsense_BE.models.logger.ErrorLogger;
import com.homeapp.nonsense_BE.models.logger.InfoLogger;
import com.homeapp.nonsense_BE.models.logger.WarnLogger;
import com.homeapp.nonsense_BE.services.BikePartsService;
import com.homeapp.nonsense_BE.services.FullBikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("FullBike/")
@CrossOrigin(origins = "http://localhost:3000")
public class BikePartsController {

    private final InfoLogger infoLogger = new InfoLogger();
    private final WarnLogger warnLogger = new WarnLogger();
    private final ErrorLogger errorLogger = new ErrorLogger();
    private final BikePartsService bikePartsService;
    private final FullBikeService fullBikeService;

    @Autowired
    public BikePartsController(BikePartsService bikePartsService, FullBikeService fullBikeService) {
        this.bikePartsService = bikePartsService;
        this.fullBikeService = fullBikeService;
    }

    @PostMapping("GetAllParts")
    public ResponseEntity<BikeParts> getAllParts(@RequestBody FullBike bike) {
        infoLogger.log("Get Bike Parts, API");
        fullBikeService.setBike(bike);
        BikeParts bikeParts = bikePartsService.getBikePartsForBike();
        if (bikeParts.getErrorMessages().size() == 0) {
            warnLogger.log("Returning Parts with ZERO errors!");
            return new ResponseEntity<>(bikeParts, HttpStatus.ACCEPTED);
        } else {
            warnLogger.log("Returning Parts with some errors...\n" + bikeParts);
            errorLogger.log("Returning Parts with some errors...\n" + bikeParts);
            return new ResponseEntity<>(bikeParts, HttpStatus.OK);
        }
    }
}