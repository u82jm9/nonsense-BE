package com.homeapp.nonsense_BE.services;

import com.homeapp.nonsense_BE.models.bike.FullBike;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

import static com.homeapp.nonsense_BE.models.bike.Enums.BrakeType.*;
import static com.homeapp.nonsense_BE.models.bike.Enums.ShifterStyle.STI;

@Service
public class ShimanoGroupsetService {

    private static final Logger LOGGER = LogManager.getLogger(ShimanoGroupsetService.class);
    private static final String chainReactionURL = "https://www.chainreactioncycles.com/p/";
    private static final String wiggleURL = "https://www.wiggle.com/p/";
    private static String link;
    private static FullBike bike;

    @Autowired
    FullBikeService fullBikeService;

    public void getShimanoGroupset() {
        if (bike.getShifterStyle().equals(STI)) {
            if (bike.getBrakeType().equals(MECHANICAL_DISC)) {
                getMechanicalSTIShifters();
            } else {
                getHydraulicSTIShifters();
            }
        } else {
            getLeverShifters();
        }
        getBrakeCalipers();
        getChainring();
        getCassette();
        getChain();
        getRearDerailleur();
        getFrontDerailleur();
    }

    private void getBrakeCalipers() {
        try {
            bike = fullBikeService.getBike();
            switch (bike.getBrakeType()) {
                case RIM -> {
                    if (bike.getRearGears().getNumberOfGears() == 8 || bike.getRearGears().getNumberOfGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3000-brake-caliper";
                    } else if (bike.getRearGears().getNumberOfGears() == 10) {
                        link = wiggleURL + "shimano-tiagra-4700-rear-brake-caliper";
                    } else {
                        link = wiggleURL + "shimano-105-r7000-brake-caliper";
                    }
                }
                case MECHANICAL_DISC -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = wiggleURL + "shimano-claris-rs305-flat-mount-disc-brake-caliper";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = wiggleURL + "shimano-sora-r317-mechanical-disc-brake-caliper";
                    } else {
                        link = wiggleURL + "trp-spyke-mechanical-disc-brake-caliper";
                    }
                }
            }
            setBikePartsFromLink(link, "Brake-Caliper");
        } catch (IOException e) {
            handleIOException("Get Brake calipers", e);
        }
    }

    private void getMechanicalSTIShifters() {
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getFrontGears().getNumberOfGears()) {
                case 1 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = wiggleURL + "microshift-r480-1x8-speed-dual-control-lever-set";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = chainReactionURL + "microshift-r490-1x9-speed-dual-control-lever-set";
                    } else if (bike.getRearGears().getNumberOfGears() == 10) {
                        link = chainReactionURL + "shimano-tiagra-4700-sti-shifter-set-2x10";
                    } else if (bike.getRearGears().getNumberOfGears() == 11) {
                        link = wiggleURL + "shimano-105-r7000-11-speed-levers";
                    } else {
                        link = wiggleURL + "shimano-ultegra-r8150-di2-12-speed-shifter-set";
                    }
                }
                case 2 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = chainReactionURL + "shimano-claris-r2000-2x8-speed-shifter-set";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = chainReactionURL + "shimano-sora-r3000-2x9-speed-gear-brake-levers";
                    } else if (bike.getRearGears().getNumberOfGears() == 10) {
                        link = chainReactionURL + "shimano-tiagra-4700-sti-shifter-set-2x10";
                    } else if (bike.getRearGears().getNumberOfGears() == 11) {
                        link = wiggleURL + "shimano-105-r7000-11-speed-levers";
                    } else {
                        link = wiggleURL + "shimano-ultegra-r8150-di2-12-speed-shifter-set";
                    }
                }
                case 3 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = chainReactionURL + "microshift-r8-3x8-speed-dual-control-lever-set";
                    } else {
                        link = chainReactionURL + "microshift-r9-3x9-speed-dual-control-levers";
                        bike.getRearGears().setNumberOfGears(9);
                        LOGGER.warn("3 by Shimano Gears are restricted to a maximum of 9 at the back");
                    }
                }
            }
            setBikePartsFromLink(link, "shifters");
        } catch (IOException e) {
            handleIOException("Get STI Shifters", e);
        }
    }

    private void getHydraulicSTIShifters() {
        try {
            bike = fullBikeService.getBike();
            if (bike.getRearGears().getNumberOfGears() == 10) {
                link = chainReactionURL + "shimano-tiagra-4725-2x10-speed-road-disc-brake";
            } else if (bike.getRearGears().getNumberOfGears() == 11) {
                link = chainReactionURL + "shimano-105-r7025-hydraulic-disc-brake";
            } else {
                link = wiggleURL + "shimano-105-r7170-di2-hydraulic-disc-brake";
            }
            setBikePartsFromLink(link, "shifters");
        } catch (
                IOException e) {
            handleIOException("Get STI Shifters", e);
        }

    }

    private void getLeverShifters() {
        LOGGER.info("No Shimano trigger shifters found ONLINE yet");
    }

    private void getChainring() {
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getFrontGears().getNumberOfGears()) {
                case 1 -> {
                    if (bike.getRearGears().getNumberOfGears() == 10 || bike.getRearGears().getNumberOfGears() == 11) {
                        link = chainReactionURL + "shimano-m5100-deore-10-11-speed-single-chainset";
                    } else if (bike.getRearGears().getNumberOfGears() == 12) {
                        link = chainReactionURL + "shimano-m6100-deore-12-speed-mtb-single-chainset";
                    } else {
                        link = chainReactionURL + "miche-xpress-track-chainset";
                    }
                }
                case 2 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = chainReactionURL + "shimano-fc-r2000-claris-compact-8-speed-chainset";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = chainReactionURL + "shimano-r3000-sora-9-speed-chainset";
                    } else if (bike.getRearGears().getNumberOfGears() == 10) {
                        link = chainReactionURL + "shimano-tiagra-4700-10-speed-chainset";
                    } else if (bike.getRearGears().getNumberOfGears() == 11) {
                        link = chainReactionURL + "shimano-105-r7000-11-speed-road-double-chainset";
                    } else if (bike.getRearGears().getNumberOfGears() == 12) {
                        link = chainReactionURL + "shimano-105-r7100-12-speed-double-chainset";
                    }
                }
                case 3 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = chainReactionURL + "shimano-claris-r2000-3x8-speed-chainset";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = chainReactionURL + "shimano-sora-r3030-9-speed-triple-chainset";
                    } else {
                        link = chainReactionURL + "shimano-tiagra-4703-10sp-road-triple-chainset";
                        bike.getRearGears().setNumberOfGears(10);
                    }
                }
            }
            setBikePartsFromLink(link, "chainring");
        } catch (IOException e) {
            handleIOException("Get Chainring", e);
        }
    }

    private void getCassette() {
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getRearGears().getNumberOfGears()) {
                case 8 -> link = wiggleURL + "shimano-hg50-8-speed-cassette";
                case 9 -> link = wiggleURL + "shimano-sora-hg400-9-speed-cassette";
                case 10 -> link = chainReactionURL + "shimano-tiagra-hg500-10-speed-road-cassette-5360107149";
                case 11 -> link = chainReactionURL + "shimano-105-r7000-11-speed-cassette";
                case 12 -> link = chainReactionURL + "shimano-105-r7100-12-speed-cassette";
            }
            setBikePartsFromLink(link, "cassette");
        } catch (IOException e) {
            handleIOException("Get Cassette", e);
        }
    }

    private void getChain() {
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getRearGears().getNumberOfGears()) {
                case 8 -> link = wiggleURL + "shimano-hg-40-6-8-speed-chain";
                case 9 -> link = wiggleURL + "shimano-xt-hg93-9-speed-chain";
                case 10 -> link = wiggleURL + "shimano-hg95-10-speed-chain";
                case 11 -> link = wiggleURL + "shimano-hg601q-105-5800-11-speed-chain";
                case 12 -> link = wiggleURL + "shimano-slx-m7100-12-speed-chain";
            }
            setBikePartsFromLink(link, "chain");
        } catch (IOException e) {
            handleIOException("Get Chain", e);
        }
    }

    private void getRearDerailleur() {
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getRearGears().getNumberOfGears()) {
                case 8 -> link = wiggleURL + "shimano-claris-r2000-8-speed-rear-derailleur";
                case 9 -> link = wiggleURL + "shimano-sora-r3000-9-speed-rear-derailleur";
                case 10 -> link = wiggleURL + "shimano-tiagra-4700-10-speed-rear-derailleur-gs";
                case 11 -> link = chainReactionURL + "shimano-105-r7000-11-speed-rear-derailleur";
                case 12 -> link = chainReactionURL + "shimano-ultegra-r8150-di2-12-speed-rear-derailleur";
            }
            setBikePartsFromLink(link, "rear-derailleur");
        } catch (IOException e) {
            handleIOException("Get Rear Derailleur", e);
        }
    }

    private void getFrontDerailleur() {
        try {
            bike = fullBikeService.getBike();
            switch ((int) bike.getFrontGears().getNumberOfGears()) {
                case 1 -> {
                    link = wiggleURL + "deda-dog-fang-chain-catcher";
                    LOGGER.info("Front Derailleur not required, providing chain catcher");
                }
                case 2 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = chainReactionURL + "microshift-r8-r252-double-front-derailleur";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3000-9-speed-double-front-derailleur";
                    } else if (bike.getRearGears().getNumberOfGears() == 10) {
                        link = wiggleURL + "shimano-tiagra-fd4700-10-speed-front-derailleur";
                    } else if (bike.getRearGears().getNumberOfGears() == 11) {
                        link = wiggleURL + "shimano-105-r7000-11-speed-front-derailleur";
                    } else if (bike.getRearGears().getNumberOfGears() == 12) {
                        link = wiggleURL + "shimano-105-r7150-di2-e-tube-front-derailleur";
                    }
                }
                case 3 -> {
                    if (bike.getRearGears().getNumberOfGears() == 8) {
                        link = wiggleURL + "microshift-r539-triple-9-speed-front-road-derailleur";
                    } else if (bike.getRearGears().getNumberOfGears() == 9) {
                        link = wiggleURL + "shimano-sora-r3030-9-speed-triple-front-derailleur";
                    } else {
                        link = wiggleURL + "shimano-tiagra-4703-3x10sp-braze-on-front-mech";
                    }
                }
            }
            setBikePartsFromLink(link, "front-derailleur");
        } catch (IOException e) {
            handleIOException("Get Front Derailleur", e);
        }
    }

    public void setBikePartsFromLink(String link, String part) throws IOException {
        bike = fullBikeService.getBike();
        Document doc = Jsoup.connect(link).get();
        Element e = doc.select("div.ProductDetail_container__Z7Hge").get(0);
        String name = Objects.requireNonNull(e.select("h1").first()).text();
        String price = Objects.requireNonNull(e.select("div.ProductPrice_productPrice__cWyiO").select("p").first()).text().replace("£", "").split(" ")[0];
        LOGGER.info("Found Product: " + name);
        LOGGER.info("For Price: " + price);
        LOGGER.info("Link: " + link);
        switch (part) {
            case "chain" -> {
                bike.getBikeParts().setChainName(name);
                bike.getBikeParts().setChainPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "bar" -> {
                bike.getBikeParts().setHandlebarName(name);
                bike.getBikeParts().setHandlebarPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "cassette" -> {
                bike.getBikeParts().setCassetteName(name);
                bike.getBikeParts().setCassettePrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "chainring" -> {
                bike.getBikeParts().setChainringName(name);
                bike.getBikeParts().setChainringPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "rear-derailleur" -> {
                bike.getBikeParts().setRearDeraileurName(name);
                bike.getBikeParts().setRearDeraileurPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "front-derailleur" -> {
                bike.getBikeParts().setFrontDeraileurName(name);
                bike.getBikeParts().setFrontDeraileurPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "brake-lever" -> {
                bike.getBikeParts().setBrakeLeverName(name);
                bike.getBikeParts().setBrakeLeverPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
            case "shifters" -> {
                bike.getBikeParts().setShifterName(name);
                bike.getBikeParts().setShifterPrice(new BigDecimal(price));
                bike.getBikeParts().getWeblinks().add(link);
            }
        }
    }

    private void handleIOException(String message, IOException e) {
        LOGGER.error("An IOException occurred from: " + message + "! " + e.getMessage());
    }
}