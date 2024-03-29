package com.homeapp.NonsenseBE.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.homeapp.NonsenseBE.models.bike.Enums.BrakeType.RIM;
import static com.homeapp.NonsenseBE.models.bike.Enums.FrameStyle.SINGLE_SPEED;
import static com.homeapp.NonsenseBE.models.bike.Enums.GroupsetBrand.SHIMANO;

/**
 * The Bike Parts Service.
 * Houses all methods relating to getting Bike Parts for a given design Bike.
 */
@Service
@Scope("singleton")
public class BikePartsService {

    private static final String chainReactionURL = "https://www.chainreactioncycles.com/p/";
    private static final String wiggleURL = "https://www.wiggle.com/p/";
    private static final String haloURL = "https://www.halowheels.com/shop/wheels/";
    private static final String dolanURL = "https://www.dolan-bikes.com/";
    private static final String genesisURL = "https://www.genesisbikes.co.uk/";
    private static final String LINKS_FILE = "src/main/resources/links.json";
    private ObjectMapper om = new ObjectMapper();
    private static FullBike bike;
    private BikeParts bikeParts;
    private final InfoLogger infoLogger = new InfoLogger();
    private final WarnLogger warnLogger = new WarnLogger();
    private final ErrorLogger errorLogger = new ErrorLogger();
    private final FullBikeService fullBikeService;
    private final ShimanoGroupsetService shimanoGroupsetService;

    public BikePartsService() {
        this.fullBikeService = null;
        this.shimanoGroupsetService = null;
    }

    /**
     * Instantiates a new Bike parts service.
     * This instantiation is Autowired to allow this Service class to use methods from the other Service classes and the Exception Handler.
     *
     * @param fullBikeService        the Full Bike Service
     * @param shimanoGroupsetService the Shimano Groupset Service
     */
    @Autowired
    public BikePartsService(FullBikeService fullBikeService, ShimanoGroupsetService shimanoGroupsetService) {
        this.fullBikeService = fullBikeService;
        this.shimanoGroupsetService = shimanoGroupsetService;
        this.bikeParts = new BikeParts();
    }

    /**
     * Gets bike parts for bike, each call of this method uses a new BikeParts object, so has no influence from previous calls.
     * Bike parts Object is set to this instance. Each update to the Bike Parts object is done on this instance.
     * Method uses the bike that is currently on the instance of the Full Bike Service.
     * Sets of each individual get part methods, in parallel to save time, then combines the results into a single return Object.
     *
     * @return the Bike Parts Object
     */
    public BikeParts getBikePartsForBike() {
        bikeParts = new BikeParts();
        bike = fullBikeService.getBike();
        CompletableFuture<Void> handleBarFuture = CompletableFuture.runAsync(this::getHandlebarParts);
        CompletableFuture<Void> frameFuture = CompletableFuture.runAsync(this::getFrameParts);
        CompletableFuture<Void> gearFuture = CompletableFuture.runAsync(this::getGearSet);
        CompletableFuture<Void> wheelFuture = CompletableFuture.runAsync(this::getWheels);
        CompletableFuture.allOf(handleBarFuture, frameFuture, gearFuture, wheelFuture).join();
        calculateTotalPrice();
        return bikeParts;
    }

    /**
     * A method that runs through the manually updated list of links in the links.json file.
     * Collects all problem links and sends these to reporter
     */
    public void checkAllLinks() {
        List<String> allLinks = readLinksFile();
        List<String> problemLinks = new ArrayList<>();
        for (String link : allLinks) {
            try {
                int statusCode = Jsoup.connect(link).execute().statusCode();
                if (statusCode != 200) {
                    problemLinks.add(link);
                }
            } catch (IOException e) {
                problemLinks.add(link);
                errorLogger.log("An IOException occurred from method: checkAllLinks!!See error message: " + e.getMessage() + "!!From link: " + link);
            }
        }
        problemLinks.forEach(entry -> errorLogger.log("Issue with link: " + entry));
    }

    private List<String> readLinksFile() {
        infoLogger.log("Reading all Links from File");
        try {
            File file = new File(LINKS_FILE);
            return om.readValue(file, new TypeReference<>() {
            });
        } catch (IOException e) {
            errorLogger.log("An IOException occurred from method: readLinksFile!!See error message: " + e.getMessage() + "!!From: " + getClass());
        }
        return new ArrayList<>();
    }

    private void getWheels() {
        String link = "";
        String component = "Wheels";
        String method = "Get Wheels";
        try {
            bike = fullBikeService.getBike();
            infoLogger.log("Method for getting Bike Wheels from Web");
            if (!bike.getFrame().getFrameStyle().equals(SINGLE_SPEED)) {
                // Wheels which require Gears are from Wiggle
                if (!bike.getBrakeType().equals(RIM)) {
                    if (bike.getWheelPreference().equals("Cheap")) {
                        link = wiggleURL + "prime-baroudeur-disc-alloy-wheelset";
                    } else {
                        link = wiggleURL + "prime-primavera-56-carbon-disc-wheelset";
                    }
                } else {
                    if (bike.getWheelPreference().equals("Cheap")) {
                        link = wiggleURL + "fulcrum-racing-4-c17-road-wheelset";
                    } else {
                        link = wiggleURL + "reynolds-aero-65-black-label-carbon-wheelset";
                    }
                }
                shimanoGroupsetService.setBikePartsFromLink(link, component, method);
            } else {
                // Wheels for Single Speed are from Halo
                if (bike.getWheelPreference().equals("Cheap")) {
                    link = haloURL + "aerorage-track-700c-wheels/";
                } else {
                    link = haloURL + "carbaura-crit-700c-wheelset/";
                }
                String wheelPrice;
                String wheelName;
                Document doc = Jsoup.connect(link).timeout(5000).get();
                Optional<Element> e = Optional.of(doc.select("div.productDetails").get(0));
                if (e.isEmpty()) {
                    bikeParts.getErrorMessages().add(new Error(component, method, link));
                    errorLogger.log("An Error occurred from: " + method + "!!Connecting to link: " + link + "!!For bike Component: " + component);
                } else {
                    Element e1 = e.get();
                    wheelName = e1.select("h1").first().text();
                    if (e1.select("div.priceSummary").select("ins").first() != null) {
                        wheelPrice = e1.select("div.priceSummary").select("ins").select("span").first().text().replace("£", "").split(" ")[0];
                    } else {
                        wheelPrice = e1.select("div.priceSummary").select("span").first().text().replace("£", "").split(" ")[0];
                    }
                    e1.select("div.priceSummary").select("ins").first();
                    if (!wheelPrice.contains(".")) {
                        wheelPrice = wheelPrice + ".00";
                    }
                    warnLogger.log("Found Product: " + wheelName);
                    warnLogger.log("For Price: " + wheelPrice);
                    warnLogger.log("Link: " + link);
                    bikeParts.getListOfParts().add(new Part("Wheel Set", wheelName, wheelPrice, link));
                }
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!For link: " + link + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getGearSet() {
        bike = fullBikeService.getBike();
        bike.setGroupsetBrand(SHIMANO);
        shimanoGroupsetService.getShimanoGroupset(bikeParts);
    }

    private void getHandlebarParts() {
        String link = "";
        String component = "HandleBars";
        String method = "GetHandleBarParts";
        try {
            bike = fullBikeService.getBike();
            infoLogger.log("Method for Getting Handlebar Parts from web");
            switch (bike.getHandleBarType()) {
                case DROPS -> link = chainReactionURL + "prime-primavera-x-light-pro-carbon-handlebar";
                case FLAT -> link = chainReactionURL + "nukeproof-horizon-v2-alloy-riser-handlebar-35mm";
                case BULLHORNS -> link = chainReactionURL + "cinelli-bullhorn-road-handlebar";
                case FLARE -> link = chainReactionURL + "ritchey-comp-venturemax-handlebar";
            }
            shimanoGroupsetService.setBikePartsFromLink(link, component, method);
        } catch (Exception e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An Exception occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    private void getFrameParts() {
        String link = "";
        String component = "Frame";
        String method = "GetFrameParts";
        try {
            bike = fullBikeService.getBike();
            infoLogger.log("Jsoup Method for Getting Frame Parts");
            String frameName = "";
            String framePrice = "";
            Optional<Element> e = Optional.empty();
            Document doc;
            switch (bike.getFrame().getFrameStyle()) {
                case ROAD -> {
                    if (bike.getFrame().isDiscBrakeCompatible()) {
                        link = dolanURL + "dolan-rdx-aluminium-disc--frameset/";
                    } else {
                        link = dolanURL + "dolan-preffisio-aluminium-road--frameset/";
                    }
                }
                case TOUR -> {
                    if (bike.getFrame().isDiscBrakeCompatible()) {
                        link = genesisURL + "genesis-fugio-frameset-vargn22330/";
                    } else {
                        link = genesisURL + "genesis-equilibrium-725-frameset-vargn21810";
                    }
                }
                case GRAVEL -> {
                    link = dolanURL + "dolan-gxa2020-aluminium-gravel-frameset/";
                }
                case SINGLE_SPEED -> {
                    link = dolanURL + "dolan-pre-cursa-aluminium-frameset/";
                }
            }
            doc = Jsoup.connect(link).timeout(5000).get();
            if (link.contains("dolan-bikes")) {
                e = Optional.of(doc.select("div.productBuy > div.productPanel").first());
                if (e.isEmpty()) {
                    errorLogger.log("An Error occurred from: " + method + "!!Connecting to link: " + link + "!!For bike Component: " + component);
                } else {
                    frameName = e.get().select("h1").first().text();
                    framePrice = e.get().select("div.price").select("span.price").first().text();
                }
            } else if (link.contains("genesisbikes")) {
                e = Optional.of(doc.select("div.product-info-main-header").first());
                if (e.isEmpty()) {
                    bikeParts.getErrorMessages().add(new Error(component, method, link));
                    errorLogger.log("An Error occurred from: " + method + "!!Connecting to link: " + link + "!!For bike Component: " + component);
                } else {
                    frameName = e.get().select("h1.page-title").text();
                    framePrice = e.get().select("div.product-info-price > div.price-final_price").first().select("span").text();
                }
            }
            if (e.isPresent()) {
                framePrice = framePrice.replaceAll("[^\\d.]", "");
                framePrice = framePrice.split("\\.")[0] + "." + framePrice.split("\\.")[1].substring(0, 2);
                if (!framePrice.contains(".")) {
                    framePrice = framePrice + ".00";
                }
                bikeParts.getListOfParts().add(new Part("Frame", frameName, framePrice, link));
                warnLogger.log("Found Frame: " + frameName);
                warnLogger.log("For price: " + framePrice);
                warnLogger.log("Frame link: " + link);
            }
        } catch (IOException e) {
            bikeParts.getErrorMessages().add(new Error(component, method, e.getMessage()));
            errorLogger.log("An IOException occurred from: " + method + "!!See error message: " + e.getMessage() + "!!For bike Component: " + component);
        }
    }

    /**
     * Takes the price of each part on the bike parts instance object and sums them to create a total price.
     * Restructures the big decimal value into a String for displaying on FE.
     */
    private void calculateTotalPrice() {
        BigDecimal total = new BigDecimal(0);
        for (Part p : bikeParts.getListOfParts()) {
            p.setPrice(p.getPrice().replace(",", ""));
            BigDecimal bd = new BigDecimal(p.getPrice());
            bd = bd.setScale(2, RoundingMode.CEILING);
            total = total.add(bd);
        }
        bikeParts.setTotalBikePrice(total);
        bikeParts.setTotalPriceAsString(NumberFormat.getCurrencyInstance(Locale.UK).format(total));

    }
}