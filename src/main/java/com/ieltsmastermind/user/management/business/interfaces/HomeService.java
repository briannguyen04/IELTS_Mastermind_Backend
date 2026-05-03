package com.ieltsmastermind.user.management.business.interfaces;

import com.ieltsmastermind.user.management.domain.dto.HomePageResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface HomeService {

    HomePageResponseDto getHomeUserInfo(HttpServletRequest request);

}