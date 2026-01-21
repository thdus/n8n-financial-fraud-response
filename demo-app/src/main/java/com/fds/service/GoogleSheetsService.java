package com.fds.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    @Value("${google.sheets.user-risk-range}")
    private String userRiskRange;

    /**
     * Google Sheets에서 특정 사용자의 Current_Total_Score 조회
     */
    public Integer getCurrentTotalScore(String userId) {
        try {
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, userRiskRange)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                log.warn("No data found in Google Sheets");
                return 0;
            }

            // 헤더 행 건너뛰기 (1행부터 시작)
            for (int i = 1; i < values.size(); i++) {
                List<Object> row = values.get(i);

                // A열: User_ID, B열: Current_Total_Score
                if (row.size() >= 2) {
                    String sheetUserId = row.get(0).toString();

                    if (userId.equals(sheetUserId)) {
                        try {
                            Object scoreObj = row.get(1);
                            int score = scoreObj instanceof Number
                                    ? ((Number) scoreObj).intValue()
                                    : Integer.parseInt(scoreObj.toString());

                            log.info("Found score for user {}: {}", userId, score);
                            return score;
                        } catch (NumberFormatException e) {
                            log.error("Invalid score format for user {}: {}", userId, row.get(1));
                            return 0;
                        }
                    }
                }
            }

            log.warn("User {} not found in Google Sheets", userId);
            return 0;

        } catch (IOException e) {
            log.error("Error reading from Google Sheets for user {}", userId, e);
            return 0;
        }
    }
}