package com.coders.boardgame.domain.user.service;

import com.coders.boardgame.domain.user.dto.SchoolInfoDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolService {

    private final RestClient schoolSearchClient;

    private final String CATEGORY_CODE = "SC4";


    private final ObjectMapper objectMapper;

    public List<SchoolInfoDto> searchSchools(String query){

        // 카카오 api로 학교 데이터 불러오기
        List<Map<String, Object>> documents = fetchSchoolData(query);

        // 초등학교, 중학교 데이터만
        List<Map<String, Object>> filteredDocuments = filterSchools(documents, query);

        // 필요한 정보만 가져오기
        return mapToSchoolInfoDtos(filteredDocuments);
    }

    // 학교 데이터 불러오는 함수
    private List<Map<String, Object>> fetchSchoolData(String query) {
        final int MAX_PAGE = 3;
        List<Map<String,Object>> allDocuments = new ArrayList<>();

        for(int page =1; page <= MAX_PAGE; page++) {
            final int currentPage = page;
            // 카카오 API 호출 후 'documents' 필드에서 모든 필드가 포함된 응답을 받음
            Map<String, Object> response = schoolSearchClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", query)
                            .queryParam("category_group_code", CATEGORY_CODE)
                            .queryParam("page", currentPage)
                            .build()
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });


            // response가 null이 아닌지 확인하고 documents 필드를 안전하게 추출
            List<Map<String, Object>> documents = Optional.ofNullable(response)
                    .map(resp -> objectMapper.convertValue(resp.get("documents"),
                            new TypeReference<List<Map<String, Object>>>() {
                            }))
                    .orElse(List.of());  // response 또는 documents가 null일 경우 빈 리스트 반환

            allDocuments.addAll(documents);

            Optional<Map<String, Object>> meta = Optional.ofNullable((Map<String, Object>) response.get("meta"));
            if (meta.isPresent() && Boolean.TRUE.equals(meta.get().get("is_end"))) {
                break;
            }
        }
        return allDocuments;
    }

    private List<Map<String, Object>> filterSchools(List<Map<String, Object>> documents, String query) {
        // 부분 일치를 허용하기 위해 검색어를 여러 부분으로 분할
        String[] queryParts = query.split("\\s+");

        return documents.stream()
                .filter(document -> {
                    String categoryName = (String) document.get("category_name");
                    String placeName = (String) document.get("place_name");
                    String addressName = (String) document.get("address_name");
                    String roadAddressName = (String) document.get("road_address_name");

                    // 초등학교 또는 중학교로 분류되는지 확인
                    boolean isSchoolCategory = categoryName != null &&
                            (categoryName.contains("초등학교") || categoryName.contains("중학교"));

                    // 검색어의 모든 부분이 장소명, 주소명 또는 도로명 주소에 일치하는지 확인
                    boolean isMatchingPlaceName = isMatchingAnyField(queryParts, placeName, addressName, roadAddressName);

                    return isSchoolCategory && isMatchingPlaceName;
                })
                .collect(Collectors.toList());
    }

// 주어진 필드에서 모든 검색어 부분이 일치하는지 확인하는 보조 메서드
    private boolean isMatchingAnyField(String[] queryParts, String... fields) {
        for (String part : queryParts) {
            boolean partFoundInAnyField = false;
            for (String field : fields) {
                if (field != null && field.contains(part)) {
                    partFoundInAnyField = true;
                    break;
                }
            }
            if (!partFoundInAnyField) {
                return false;
            }
        }
        return true;
    }
    private List<SchoolInfoDto> mapToSchoolInfoDtos(List<Map<String, Object>> documents) {
        return documents.stream()
                .map(document -> {
                    SchoolInfoDto schoolInfo = new SchoolInfoDto();
                    schoolInfo.setName((String) document.get("place_name"));
                    schoolInfo.setRoadAddress((String) document.get("road_address_name"));
                    schoolInfo.setDistrictAddress((String) document.get("address_name"));
                    return schoolInfo;
                })
                .collect(Collectors.toList());
    }
}
