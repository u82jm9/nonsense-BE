package com.homeapp.NonsenseBE.services;

import com.homeapp.NonsenseBE.models.bike.BikeParts;
import com.homeapp.NonsenseBE.models.bike.Error;
import com.homeapp.NonsenseBE.models.bike.FullBike;
import com.homeapp.NonsenseBE.models.bike.Part;
import com.homeapp.NonsenseBE.models.logger.ErrorLogger;
import com.homeapp.NonsenseBE.models.logger.InfoLogger;
import com.homeapp.NonsenseBE.models.logger.WarnLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.homeapp.NonsenseBE.models.bike.Enums.BrakeType.*;
import static com.homeapp.NonsenseBE.models.bike.Enums.ShifterStyle.STI;

/**
 * The type Shimano groupset service.
 */
@Service
public class ShimanoGroupsetService {
    private static final String chainReactionURL = "https://www.chainreactioncycles.com/p/";
    private static final String wiggleURL = "https://www.wiggle.com/p/";
    private static FullBike bike;
    private BikeParts bikeParts;
    private final InfoLogger infoLogger = new InfoLogger();
    private final WarnLogger warnLogger = new WarnLogger();
    private final ErrorLogger errorLogger = new ErrorLogger();
    private final FullBikeService fullBikeService;


    /**
     * Instantiates a new Shimano Groupset Service.
     * Autowires in a FullBike Service to allow for access to instance Bike in all methods within this class.
     *
     * @param fullBikeService the full bike service
     */
    @Autowired
    public ShimanoGroupsetService(@Lazy FullBikeService fullBikeService) {
        this.fullBikeService = fullBikeService;
    }

    /**
     * Gets shimano groupset.
     * Sets the passed-in parts to the instance object, to be used throughout the class.
     * Runs each separate component method in parallel to improve efficiency.
     * Each component method chooses the correct link based on design bike and then retrieves the individual part information from the web.
     *
     * @param parts the parts
     */
    public void getShimanoGroupset(BikeParts parts) {
        infoLogger.log("Getting Parts for Shimano Groupset.");
        bikeParts = parts;
        bike = fullBikeService.getBike();
        if (!bike.getShifterStyle().equals(STI)) {
            getLeverShifters();
            getBrakeLevers();
        } else {
            if ((bike.getBrakeType().equals(MECHANICAL_DISC)) || (bike.getBrakeType().equals(RIM))) {
                getMechanicalSTIShifters();
            } else if (bike.getBrakeType().equals(HYDRAULIC_DISC)) {
                getHydraulicSTIShifters();
            }
        }
        CompletableFuture<Void> brakeFuture = CompletableFuture.runAsync(this::getBrakeCalipers);
        CompletableFuture<Void> chainringFuture = CompletableFuture.runAsync(this::getChainring);
        CompletableFuture<Void> cassetteFuture = CompletableFuture.runAsync(this::getCassette);
        CompletableFuture<Void> chainFuture = CompletableFuture.runAsync(this::getChain);
        CompletableFuture<Void> rearDerailleurFuture = CompletableFuture.runAsync(this::getRearDerailleur);
        CompletableFuture<Void> frontDerailleurFuture = CompletableFuture.runAsync(this::getFrontDerailleur);
        CompletableFuture.allOf(brakeFuture, chainringFuture, cassetteFuture, chainFuture, rearDerailleurFuture, frontDerailleurFuture).join();
        if (!bikeParts.getErrorMessages().isEmpty()) {
            errorLogger.log("BikeParts has errors: " + bikeParts.getErrorMessages());
        }
    }

    private void getBrakeLevers() {
        String link = "";
        String component = "Brake-Levers";
        String method = "getBrakeLevers";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            if (bike.getBrakeType().equals(HYDRAULIC_DISC)) {
                link = wiggleURL + "shimano-grx-812-sub-brake-lever";
                setBikePartsFromLink(link, "Left " + component, method);
                setBikePartsFromLink(link, "Right " + component, method);
            } else {
                link = wiggleURL + "shimano-deore-t610-v-brake-levers";
                setBikePartsFromLink(link, component, method);
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getBrakeCalipers() {
        String link = "";
        String component = "Brake-Caliper";
        String method = "getBrakeCalipers";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch (bike.getBrakeType()) {
                case RIM -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3000-brake-caliper";
                    } else if (bike.getNumberOfRearGears() == 10) {
                        link = wiggleURL + "shimano-tiagra-4700-rear-brake-caliper";
                    } else {
                        link = chainReactionURL + "shimano-105-r7000-brake-caliper";
                    }
                }
                case MECHANICAL_DISC -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = wiggleURL + "shimano-sora-r317-mechanical-disc-brake-caliper";
                    } else {
                        link = wiggleURL + "trp-spyke-mechanical-disc-brake-caliper";
                    }
                }
                default -> {
                    warnLogger.log("Not getting link for calipers as Hydraulic calipers and levers are together");
                }
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, "Front " + component, method);
                setBikePartsFromLink(link, "Rear " + component, method);
            } else {
                if (!bike.getBrakeType().equals(HYDRAULIC_DISC)) {
                    bikeParts.getErrorMessages().add(new Error(component, method, link));
                }
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getMechanicalSTIShifters() {
        String link = "";
        String component = "STI-Shifter";
        String method = "getMechanicalSTIShifters";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getNumberOfFrontGears()) {
                case 1 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = chainReactionURL + "microshift-advent-m090-1x9-speed-gear-brake-levers";
                    } else if (bike.getNumberOfRearGears() == 10) {
                        link = chainReactionURL + "shimano-tiagra-4700-sti-shifter-set-2x10";
                    } else if (bike.getNumberOfRearGears() == 11) {
                        link = wiggleURL + "shimano-105-r7000-11-speed-levers";
                    } else {
                        link = wiggleURL + "shimano-ultegra-r8150-di2-12-speed-shifter-set";
                    }
                }
                case 2 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = chainReactionURL + "shimano-sora-r3000-2x9-speed-gear-brake-levers";
                    } else if (bike.getNumberOfRearGears() == 10) {
                        link = chainReactionURL + "shimano-tiagra-4700-sti-shifter-set-2x10";
                    } else if (bike.getNumberOfRearGears() == 11) {
                        link = wiggleURL + "shimano-105-r7000-11-speed-levers";
                    } else {
                        link = wiggleURL + "shimano-ultegra-r8150-di2-12-speed-shifter-set";
                    }
                }
                case 3 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = chainReactionURL + "microshift-r9-3x9-speed-dual-control-levers";
                    } else {
                        link = wiggleURL + "shimano-tiagra-4700-3x10-speed-lever-set";
                        bike.setNumberOfRearGears(10);
                        warnLogger.log("3 by Shimano Gears are restricted to a maximum of 10 at the back");
                    }
                }
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getHydraulicSTIShifters() {
        String link = "";
        String component = "Hydraulic-Shifter";
        String method = "getHydraulicSTIShifters";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            if (bike.getNumberOfRearGears() == 10) {
                link = chainReactionURL + "shimano-tiagra-4725-2x10-speed-road-disc-brake";
            } else if (bike.getNumberOfRearGears() == 11) {
                link = chainReactionURL + "shimano-105-r7025-hydraulic-disc-brake";
            } else if (bike.getNumberOfRearGears() == 12) {
                link = wiggleURL + "shimano-105-r7170-di2-hydraulic-disc-brake";
            } else {
                link = wiggleURL + "clarks-m2-hydraulic-disc-brake-with-rotor";
            }
            setBikePartsFromLink(link, "Right " + component, method);
            if (bike.getNumberOfFrontGears() == 1) {
                link = chainReactionURL + "shimano-grx-820-hydraulic-drop-bar-brake-lever";
            }
            setBikePartsFromLink(link, "Left " + component, method);
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getLeverShifters() {
        String link = "";
        String component = "Trigger-Shifter";
        String method = "getLeverShifters";
        infoLogger.log("Getting Parts for: " + component);
        try {
            switch ((int) bike.getNumberOfRearGears()) {
                case 10 -> link = wiggleURL + "shimano-deore-m6000-10-speed-trigger-shifter";
                case 11 -> link = wiggleURL + "shimano-xt-m8000-11-speed-trigger-shifter";
                default -> link = chainReactionURL + "shimano-altus-m2010-9-speed-shifter";
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getChainring() {
        String link = "";
        String component = "Chainring";
        String method = "getChainring";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getNumberOfFrontGears()) {
                case 1 -> {
                    if (bike.getNumberOfRearGears() == 10 || bike.getNumberOfRearGears() == 11) {
                        link = chainReactionURL + "shimano-m5100-deore-10-11-speed-single-chainset";
                    } else if (bike.getNumberOfRearGears() == 12) {
                        link = chainReactionURL + "shimano-m6100-deore-12-speed-mtb-single-chainset";
                    } else {
                        link = wiggleURL + "miche-primato-advanced-track-single-chainset";
                    }
                }
                case 2 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = chainReactionURL + "shimano-claris-r2000-8-speed-double-chainset";
                    } else if (bike.getNumberOfRearGears() == 10) {
                        link = chainReactionURL + "shimano-tiagra-4700-10-speed-chainset";
                    } else if (bike.getNumberOfRearGears() == 11) {
                        link = chainReactionURL + "shimano-105-r7000-11-speed-road-double-chainset";
                    } else if (bike.getNumberOfRearGears() == 12) {
                        link = chainReactionURL + "shimano-105-r7100-12-speed-double-chainset";
                    }
                }
                case 3 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3030-9-speed-triple-chainset";
                    } else {
                        link = chainReactionURL + "shimano-tiagra-4703-10sp-road-triple-chainset";
                        bike.setNumberOfRearGears(10);
                    }
                }
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getCassette() {
        String link = "";
        String component = "Cassette";
        String method = "getCassette";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getNumberOfRearGears()) {
                case 9 -> link = wiggleURL + "shimano-sora-hg400-9-speed-cassette";
                case 10 -> link = chainReactionURL + "shimano-tiagra-hg500-10-speed-road-cassette-5360107149";
                case 11 -> link = chainReactionURL + "shimano-105-r7000-11-speed-cassette";
                case 12 -> link = chainReactionURL + "shimano-105-r7100-12-speed-cassette";
                default -> link = wiggleURL + "shimano-dx-single-speed-sprocket";
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getChain() {
        String link = "";
        String component = "Chain";
        String method = "getChain";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getNumberOfRearGears()) {
                case 9 -> link = wiggleURL + "shimano-xt-hg93-9-speed-chain";
                case 10 -> link = wiggleURL + "shimano-hg95-10-speed-chain";
                case 11 -> link = wiggleURL + "shimano-hg601q-105-5800-11-speed-chain";
                case 12 -> link = wiggleURL + "shimano-slx-m7100-12-speed-chain";
                default -> link = chainReactionURL + "shimano-nexus-single-speed-chain";
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getRearDerailleur() {
        String link = "";
        String component = "Rear-Derailleur";
        String method = "getRearDerailleur";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getNumberOfRearGears()) {
                case 9 -> link = wiggleURL + "shimano-sora-r3000-9-speed-rear-derailleur";
                case 10 -> link = wiggleURL + "shimano-tiagra-4700-10-speed-rear-derailleur-gs";
                case 11 -> link = chainReactionURL + "shimano-105-r7000-11-speed-rear-derailleur";
                case 12 -> link = chainReactionURL + "shimano-ultegra-r8150-di2-12-speed-rear-derailleur";
                default -> {
                }
            }
            if (!link.isEmpty() && bike.getNumberOfRearGears() > 1) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getFrontDerailleur() {
        String link = "";
        String component = "Front-Derailleur";
        String method = "getFrontDerailleur";
        infoLogger.log("Getting Parts for: " + component);
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getNumberOfFrontGears()) {
                case 1 -> {
                    link = wiggleURL + "deda-dog-fang-chain-catcher";
                    warnLogger.log("Front Derailleur not required, providing chain catcher");
                }
                case 2 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3000-9-speed-double-front-derailleur";
                    } else if (bike.getNumberOfRearGears() == 10) {
                        link = wiggleURL + "shimano-tiagra-fd4700-10-speed-front-derailleur";
                    } else if (bike.getNumberOfRearGears() == 11) {
                        link = wiggleURL + "shimano-105-r7000-11-speed-front-derailleur";
                    } else if (bike.getNumberOfRearGears() == 12) {
                        link = wiggleURL + "shimano-105-r7150-di2-e-tube-front-derailleur";
                    }
                }
                case 3 -> {
                    if (bike.getNumberOfRearGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3030-9-speed-triple-front-derailleur";
                    } else {
                        link = wiggleURL + "shimano-tiagra-4703-3x10sp-braze-on-front-mech";
                    }
                }
            }
            if (!link.isEmpty()) {
                setBikePartsFromLink(link, component, method);
            } else {
                bikeParts.getErrorMessages().add(new Error(component, method, link));
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    /**
     * Sets bike parts from link passed-in.
     * Majority of bike component information is coming from the same website.
     * Single method used to access website and skim information. This is then used to populate Part Object.
     * BikeParts Object on instance is then updated with the new Part object.
     *
     * @param link      the link
     * @param component the component
     * @param method    the method
     * @throws IOException the io exception
     */
    public void setBikePartsFromLink(String link, String component, String method) throws IOException {
        try {
            bike = fullBikeService.getBike();
            infoLogger.log("Connecting to link: " + link);
            infoLogger.log("For Component: " + component);
            Document doc = Jsoup.connect(link).timeout(5000).get();
            Optional<Element> e = Optional.ofNullable(doc.select("div.ProductDetail_container__FX6xF").get(0));
            if (e.isEmpty()) {
                linkElementNotFound(component, method, link);
            } else {
                linkElementFound(component, e.get(), link);
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, link));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void linkElementNotFound(String component, String method, String link) {
        bikeParts.getErrorMessages().add(new Error(component, method, link));
        errorLogger.log("An Error occurred from: " + method + "!!Connecting to link: " + link + "!!For bike Component: " + component);
    }

    private void linkElementFound(String component, Element e, String link) {
        String name = e.select("h1").first().text();
        String price = e.select("div.ProductPrice_productPrice__Fg1nA")
                .select("p").first().text().replace("£", "").split(" ")[0];
        if (!price.contains(".")) {
            price = price + ".00";
        }
        warnLogger.log("Found Product: " + name);
        warnLogger.log("For Price: " + price);
        warnLogger.log("Link: " + link);
        bikeParts.getListOfParts().add(new Part(component, name, price, link));
    }
}