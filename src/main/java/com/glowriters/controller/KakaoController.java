package com.glowriters.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowriters.domain.KakaoDTO;
import com.glowriters.domain.Member;
import com.glowriters.service.KakaoService;
import com.glowriters.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class KakaoController {
	private final KakaoService kakaoService;
	private final ObjectMapper objectMapper;

	@Autowired
	private final MemberService memberService;

	// 메인뷰에서 로그인 버튼을 눌렀을때
	@GetMapping("/oauth")
	public String callback(HttpServletRequest request) throws Exception {
		// 카카오로부터 받은 인증코드를 통해 사용자정보를담은 domain객체를 생성(서비스호출)
		KakaoDTO kakaoInfo = kakaoService.getKakaoInfo(request.getParameter("code"));
		Member member = new Member();
		member.setMember_hashcode(String.valueOf(kakaoInfo.getId()));
		member.setMember_email(kakaoInfo.getEmail());
		member.setMember_nickname(kakaoInfo.getNickname());
		member.setMember_profile(kakaoInfo.getProfileImage());

		// member 테이블에 이미 회원으로 존재하는지 알아보기 위한 코드
		List<Member> members = memberService.findAll();
		boolean istrue = false;

		for (Member m : members) {
			if (m.getMember_hashcode().equals(member.getMember_hashcode())) {
				istrue = true;
				break;
			}
		}

		if (istrue == false) {
			memberService.save(member);
		}

		// 로그인 정보를 세션에 담는다.
		HttpSession session = request.getSession();
		long member_id = memberService.findByHashCode(member.getMember_hashcode()).getMember_id();
		session.setAttribute("member_hashcode", member.getMember_hashcode());
		session.setAttribute("member_id", member_id);
		session.setAttribute("accessToken", kakaoInfo.getAccessToken());
		session.setAttribute("refreshToken", kakaoInfo.getRefreshToken());
		return "redirect:/"; // main으로 리다이렉트
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		String accessToken = String.valueOf(session.getAttribute("accessToken"));
		
		//액세스토큰이 존재할때
		if(accessToken != null && !"".equals(accessToken)) {
			try {
				//실제 로그아웃 요청을 보내는 코드
				kakaoService.kakaoDisconnect(accessToken);
			}catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
			//로그아웃 진행 : 세션에서 관련된 모든 정보 지움
			session.removeAttribute("member_hashcode");
			session.removeAttribute("member_id");
			session.removeAttribute("accessToken");
			session.removeAttribute("refreshToken");
			session.removeAttribute("isLogin"); //로그인했다는 정보도 지움
		}else {
			log.warn("엑세스 토큰이 비었습니다.!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
		
		return "redirect:/";
	}
}