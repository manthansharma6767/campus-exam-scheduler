package com.manthan.campusexamscheduler.dto;

import java.util.Map;

public record ImportPreviewRow(int rowNumber, Map<String, String> values) { }
