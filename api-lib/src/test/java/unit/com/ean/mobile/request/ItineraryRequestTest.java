/*
 * Copyright 2013 EAN.com, L.P. All rights reserved.
 */
package com.ean.mobile.request;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.ean.mobile.ConfirmationStatus;
import com.ean.mobile.CustomerAddress;
import com.ean.mobile.Itinerary;
import com.ean.mobile.JSONFileUtil;
import com.ean.mobile.NightlyRate;
import com.ean.mobile.Rate;
import com.ean.mobile.exception.EanWsError;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ItineraryRequestTest {

    private ItineraryRequest itineraryRequest;

    @Before
    public void setUp() {
        itineraryRequest = new ItineraryRequest(1234L, "test@expedia.com");
    }

    @Test
    public void testConsumeNullJson() throws Exception {
        assertNull(itineraryRequest.consume(null));
    }

    @Test(expected = JSONException.class)
    public void testConsumeEmptyJson() throws Exception {
        itineraryRequest.consume(new JSONObject());
    }

    @Test(expected = JSONException.class)
    public void testConsumeInvalidJson() throws Exception {
        itineraryRequest.consume(JSONFileUtil.loadJsonFromFile("invalid-itinerary.json"));
    }

    @Test(expected = EanWsError.class)
    public void testConsumeEanWsError() throws Exception {
        itineraryRequest.consume(JSONFileUtil.loadJsonFromFile("error-itinerary.json"));
    }

    @Test
    public void testConsume() throws Exception {
        Itinerary itinerary = itineraryRequest.consume(JSONFileUtil.loadJsonFromFile("valid-itinerary.json"));
        assertNotNull(itinerary);
        assertEquals(107730857L, itinerary.id);
        assertEquals(55505L, itinerary.affiliateId);
        assertEquals(DateModifier.getDateFromString("01/28/2013"), itinerary.creationDate);
        assertEquals(DateModifier.getDateFromString("02/07/2013"), itinerary.itineraryStartDate);
        assertEquals(DateModifier.getDateFromString("02/10/2013"), itinerary.itineraryEndDate);

        doCustomerAssertions(itinerary.customer);
        doHotelConfirmationAssertions(itinerary.hotelConfirmations);
    }

    @Test
    public void testGetUri() throws Exception {
        StringBuilder queryString = new StringBuilder(119);
        queryString.append("cid=55505&apiKey=cbrzfta369qwyrm9t5b8y8kf&minorRev=20&customerUserAgent=Android");
        queryString.append("&itineraryId=1234&email=test@expedia.com");

        final URI uri = itineraryRequest.getUri();
        assertEquals("http", uri.getScheme());
        assertEquals("api.ean.com", uri.getHost());
        assertEquals("/ean-services/rs/hotel/v3/itin", uri.getPath());
        assertEquals(queryString.toString(), uri.getQuery());
    }

    @Test
    public void testIsSecure() {
        assertThat(itineraryRequest.requiresSecure(), is(false));
    }

    private void doCustomerAssertions(final Itinerary.Customer customer) {
        assertEquals("test", customer.name.first);
        assertEquals("tester", customer.name.last);
        assertEquals("test@expedia.com", customer.email);
        assertEquals("1234567890", customer.homePhone);

        CustomerAddress customerAddress = customer.address;
        assertNotNull(customerAddress);
        assertNotNull(customerAddress.lines);
        assertThat(customerAddress.lines.size(), greaterThan(0));
        assertEquals("travelnow", customerAddress.lines.get(0));
        assertEquals("Seattle", customerAddress.city);
        assertEquals("WA", customerAddress.stateProvinceCode);
        assertEquals("98004", customerAddress.postalCode);
        assertEquals(1, customerAddress.type.typeId);
        assertThat(customerAddress.isPrimary, is(true));
    }

    private void doHotelConfirmationAssertions(final List<Itinerary.HotelConfirmation> hotelConfirmations) {
        assertNotNull(hotelConfirmations);
        assertThat(hotelConfirmations.size(), greaterThan(0));

        Itinerary.HotelConfirmation hotelConfirmation = hotelConfirmations.get(0);
        assertNotNull(hotelConfirmation);
        assertEquals("7220", hotelConfirmation.roomTypeCode);
        assertEquals(13, hotelConfirmation.supplierId);
        assertEquals(ConfirmationStatus.CONFIRMED, hotelConfirmation.status);
        assertEquals(DateModifier.getDateFromString("02/07/2013"), hotelConfirmation.arrivalDate);
        assertEquals(DateModifier.getDateFromString("02/10/2013"), hotelConfirmation.departureDate);
        assertEquals("en_US", hotelConfirmation.locale);
        assertEquals("1234", hotelConfirmation.confirmationNumber);
        assertEquals("N", hotelConfirmation.smokingPreference);
        assertEquals("7220", hotelConfirmation.rateCode);
        assertEquals(1, hotelConfirmation.occupancy.numberOfAdults);
        assertThat(hotelConfirmation.occupancy.childAges.size(), equalTo(0));
        assertEquals("7-Day Advance Purchase Special (on select nights)", hotelConfirmation.rateDescription);
        assertEquals("EP", hotelConfirmation.chainCode);
        assertEquals(3, hotelConfirmation.nights);
        assertEquals("Queen of Art", hotelConfirmation.roomDescription);
        assertEquals("test", hotelConfirmation.guestName.first);
        assertEquals("tester", hotelConfirmation.guestName.last);

        doRateAssertions(hotelConfirmation.rate);
    }

    private void doRateAssertions(final Rate rate) {
        assertNotNull(rate);
        assertThat(rate.promo, is(false));

        Rate.RateInfo chargeableRateInfo = rate.chargeable;
        assertNotNull(chargeableRateInfo);
        assertEquals(new BigDecimal("79.96"), chargeableRateInfo.getSurchargeTotal());
        assertEquals(new BigDecimal("509.96"), chargeableRateInfo.getTotal());
        assertEquals(new BigDecimal("143.33"), chargeableRateInfo.getAverageBaseRate());
        assertEquals(new BigDecimal("143.33"), chargeableRateInfo.getAverageRate());
        assertEquals("USD", chargeableRateInfo.currencyCode);

        assertNotNull(chargeableRateInfo.nightlyRates);
        assertThat(chargeableRateInfo.nightlyRates.size(), equalTo(3));

        NightlyRate nightlyRate = chargeableRateInfo.nightlyRates.get(0);
        assertNotNull(nightlyRate);
        assertEquals(new BigDecimal("169.0"), nightlyRate.baseRate);
        assertEquals(new BigDecimal("169.0"), nightlyRate.rate);
        assertThat(nightlyRate.promo, is(false));

        nightlyRate = chargeableRateInfo.nightlyRates.get(1);
        assertNotNull(nightlyRate);
        assertEquals(new BigDecimal("126.75"), nightlyRate.baseRate);
        assertEquals(new BigDecimal("126.75"), nightlyRate.rate);
        assertThat(nightlyRate.promo, is(false));

        nightlyRate = chargeableRateInfo.nightlyRates.get(2);
        assertNotNull(nightlyRate);
        assertEquals(new BigDecimal("134.25"), nightlyRate.baseRate);
        assertEquals(new BigDecimal("134.25"), nightlyRate.rate);
        assertThat(nightlyRate.promo, is(false));
    }

}