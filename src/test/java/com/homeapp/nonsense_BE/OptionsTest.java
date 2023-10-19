package com.homeapp.nonsense_BE;

import com.homeapp.nonsense_BE.models.bike.Options;
import com.homeapp.nonsense_BE.services.OptionsService;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class OptionsTest {

    @Autowired
    private OptionsService optionsService;

    @Test
    public void test_That_Start_New_Bike_Returns_All_Brands(){
        Options options = optionsService.startNewBike();
        System.out.println(options);
        assertEquals(options.getGroupsetBrand().size(), 3);
    }
}