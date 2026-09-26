package com.toollix.subscriptions.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service handling Chapa Ethiopia payment gateway integration.
 * Adapted directly from the automate.tech subscription microservice
 * implementation.
 */
@Service
public class ChapaPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ChapaPaymentService.class);

    private static final String CHAPA_INITIALIZE_URL = "https://api.chapa.co/v1/transaction/initialize";
    private static final String CHAPA_VERIFY_URL = "https://api.chapa.co/v1/transaction/verify/";

    private final String chapaSecretKey;
    private final String returnUrl;

    public ChapaPaymentService(
            @Value("${chapa.secret-key:CHASECK-x3BrKZLcg0Pbz9CNMuwYDlqU6LdJccPd}") String chapaSecretKey,
            @Value("${chapa.return-url:http://localhost:3000/dashboard/billing/verify}") String returnUrl) {
        this.chapaSecretKey = chapaSecretKey;
        this.returnUrl = returnUrl;
    }

    /**
     * Initializes a transaction with Chapa and returns the checkout URL.
     */
    public String generateChapaPaymentLink(String email, double amount, String txRef, Long subscriptionId)
            throws Exception {
        log.info("[CHAPA] Generating payment link for txRef={} amount={} email={}", txRef, amount, email);

        if (chapaSecretKey == null || chapaSecretKey.isEmpty()) {
            throw new IllegalStateException("Chapa API Key is missing!");
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("amount", amount);
        requestBody.put("currency", "ETB");
        requestBody.put("email", email);
        requestBody.put("tx_ref", txRef);
        // Pass both txRef and subscriptionId so the callback page can verify
        requestBody.put("return_url", returnUrl + "?txRef=" + txRef + "&subId=" + subscriptionId);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(CHAPA_INITIALIZE_URL))
                .header("Authorization", "Bearer " + chapaSecretKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject jsonResponse = new JSONObject(response.body());
            if (jsonResponse.has("data") && jsonResponse.getJSONObject("data").has("checkout_url")) {
                String checkoutUrl = jsonResponse.getJSONObject("data").getString("checkout_url");
                log.info("[CHAPA] Payment link generated successfully: {}", checkoutUrl);
                return checkoutUrl;
            } else {
                throw new Exception("Chapa response missing 'checkout_url': " + response.body());
            }
        } else {
            throw new Exception("Chapa API request failed: HTTP " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Verifies if a transaction was completed correctly using Chapa's verify API.
     */
    public boolean verifyChapaPayment(String txRef) {
        log.info("[CHAPA] Verifying transaction txRef={}", txRef);
        if (txRef == null || txRef.isEmpty()) {
            log.warn("[CHAPA] Invalid txRef: Cannot verify.");
            return false;
        }

        try {
            URL url = URI.create(CHAPA_VERIFY_URL + txRef).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + chapaSecretKey);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                boolean isSuccess = "success".equalsIgnoreCase(jsonResponse.getJSONObject("data").getString("status"));
                log.info("[CHAPA] Transaction txRef={} verification success={}", txRef, isSuccess);
                return isSuccess;
            } else {
                log.warn("[CHAPA] API Error: Response Code {}", responseCode);
            }
        } catch (Exception e) {
            log.error("[CHAPA] Error verifying Chapa payment: {}", e.getMessage(), e);
        }
        return false;
    }
}
