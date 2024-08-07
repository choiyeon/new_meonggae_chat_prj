package com.store.meonggae.meonggaeTalk.controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.store.meonggae.meonggaeTalk.domain.ChatRoomDomain;
import com.store.meonggae.meonggaeTalk.dto.ChatingRoom;
import com.store.meonggae.meonggaeTalk.dto.Message;
import com.store.meonggae.meonggaeTalk.service.ChatService;
import com.store.meonggae.meonggaeTalk.vo.ChatRoomParticipantVO;
import com.store.meonggae.meonggaeTalk.vo.ChatSendVO;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ChatController {
 
	@Autowired(required = false)
	private ChatService chatService;
	
	// 메인화면
	@GetMapping("/chat_frm")
	public String chatFrm(@CookieValue(value="memNum", defaultValue = "2") int memNum, @CookieValue(value="goodsNum", defaultValue = "") String goodsNum, Model model) {
		
		List<ChatRoomDomain> listChatRoom = chatService.searchListChatRoom(memNum);
		String nick = chatService.searchOneMyNick(memNum);
		
		model.addAttribute("memNum", memNum);
		model.addAttribute("goodsNum", goodsNum);
		model.addAttribute("listChatRoom", listChatRoom);
		model.addAttribute("nick", nick);
		
		return "meongae_talk/talk_page";
	}
	
	// 채팅방 선택 -> 채팅 내역 출력
	@GetMapping("/chat_list")
	@ResponseBody
	public String chatList(ChatRoomParticipantVO chatRoomParticipantVO) {
		return chatService.searchListChatJson(chatRoomParticipantVO);
	} // chatList
	
	// 채팅 보내기
	@PostMapping("/chat_add")
	@ResponseBody
	public String chatAdd(ChatSendVO chatSendVO) {
		return chatService.addChat(chatSendVO);
	} // chatAdd
	
	//	----------------------------------------------------
	
	// 채팅방 목록
	public static LinkedList<ChatingRoom> chatingRoomList = new LinkedList<>();
	
	//	----------------------------------------------------
	// 유틸 메서드
	
	// 방 번호로 방 찾기
	public ChatingRoom findRoom(String roomNumber) {
		ChatingRoom room = ChatingRoom.builder().roomNumber(roomNumber).build(); 
		int index = chatingRoomList.indexOf(room);
		
		if(chatingRoomList.contains(room)) {
			return chatingRoomList.get(index); 
		}
		return null;
	}
	
	
	// 쿠키에 추가
	public void addCookie(String cookieName, String cookieValue) {
		ServletRequestAttributes attr = (ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
		HttpServletResponse response = attr.getResponse();
		
		Cookie cookie = new Cookie(cookieName, cookieValue);
		
		int maxage = 60 * 60 * 24 * 7;
		cookie.setMaxAge(maxage);
		response.addCookie(cookie);
	}
	
	
	
	// 방 번호, 닉네임 쿠키 삭제
	public void deleteCookie( ) {
		ServletRequestAttributes attr = (ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
		HttpServletResponse response = attr.getResponse();
		
		Cookie roomCookie = new Cookie("roomNumber", null);
		Cookie nicknameCookie = new Cookie("nickname",null);
		
		roomCookie.setMaxAge(0);
		nicknameCookie.setMaxAge(0);
		
		response.addCookie(nicknameCookie);
		response.addCookie(roomCookie);
	}
	
	
	
	// 쿠키에서 방번호, 닉네임 찾기
	public Map<String, String> findCookie() {
		ServletRequestAttributes attr = (ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
		HttpServletRequest request = attr.getRequest();
		
		Cookie[] cookies = request.getCookies();
		String roomNumber = "";
		String nickname= "";
		
		if(cookies == null) {
			return null;
		}
		
		if(cookies != null) {
			for(int i=0;i<cookies.length;i++) {
				if("roomNumber".equals(cookies[i].getName())) {
					roomNumber = cookies[i].getValue();
				}
				if("nickname".equals(cookies[i].getName())) {
					nickname = cookies[i].getValue();
				}
			}
			
			if(!"".equals(roomNumber) && !"".equals(nickname)) {
				Map<String, String> map = new HashMap<>();
				map.put("nickname", nickname);
				map.put("roomNumber", roomNumber);
				
				return map;
			}
		}
		
		return null;
	}
	
	// 닉네임 생성
	public void createNickname(String nickname) {
		addCookie("nickname", nickname);
	}
	
	// 방 입장하기
	public boolean enterChatingRoom(ChatingRoom chatingRoom, String nickname) {
		createNickname(nickname);
		
		if(chatingRoom == null) {
			deleteCookie();
			return false;
		} else {
			LinkedList<String> users = chatingRoom.getUsers();
			users.add(nickname);
			
			addCookie("roomNumber", chatingRoom.getRoomNumber());
			return true;
		}
	}
	
	
	//	----------------------------------------------------
	
	// 컨트롤러
	
	// 메인화면
	@GetMapping("/chat")
	public String main() {
		return "chat";
	}
	
	// 채팅방 목록
	@GetMapping("/chatingRoomList")
	public ResponseEntity<?> chatingRoomList() {
		return new ResponseEntity<LinkedList<ChatingRoom>>(chatingRoomList, HttpStatus.OK);
	}
	
	
	// 방 만들기
	@PostMapping("/chatingRoom")
	public ResponseEntity<?> chatingRoom(String roomName, String nickname) {
		
		// 방을 만들고 채팅방목록에 추가
		String roomNumber = UUID.randomUUID().toString();
		ChatingRoom chatingRoom = ChatingRoom.builder()
				.roomNumber(roomNumber)
				.users(new LinkedList<>())
				.roomName(roomName)
				.build();
		
		chatingRoomList.add(chatingRoom);
		
		// 방 입장하기
		enterChatingRoom(chatingRoom, nickname);
		
		return new ResponseEntity<>(chatingRoom, HttpStatus.OK);
	}
	
	
	// 방 들어가기
	@GetMapping("/chatingRoom-enter")
	public ResponseEntity<?> EnterChatingRoom(String roomNumber, String nickname){
		
		// 방 번호로 방 찾기
		ChatingRoom chatingRoom = findRoom(roomNumber);
		
		if(chatingRoom == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			// 방 들어가기
			enterChatingRoom(chatingRoom, nickname);
			
			return new ResponseEntity<>(chatingRoom, HttpStatus.OK);
		}
	}
	
	// 방 나가기
	@PatchMapping("/chatingRoom-exit")
	public ResponseEntity<?> ExitChatingRoom(){
			
		Map<String, String> map = findCookie();
			
		if(map == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		
		String roomNumber = map.get("roomNumber");
		String nickname = map.get("nickname");
		
		// 방목록에서 방번호에 맞는 유저목록 가져오기
		ChatingRoom chatingRoom = findRoom(roomNumber);
		List<String> users = chatingRoom.getUsers();
		
		// 닉네임 삭제
		users.remove(nickname);
		
		// 쿠키에서 닉네임과 방번호 삭제
		deleteCookie();
		
		// 유저가 한명도 없다면 방 삭제
		if(users.size() == 0) {
			chatingRoomList.remove(chatingRoom);
		}
		
		return new ResponseEntity<>(chatingRoom, HttpStatus.OK);
	}
	
	
	// 참가 중이었던 대화방
	@GetMapping("/chatingRoom")
	public ResponseEntity<?> chatingRoom() {
		// 쿠키에 닉네임과 방번호가 있다면 대화중이던 방이 있던것
		Map<String, String> map = findCookie();
		
		if(map == null) {
			return new ResponseEntity<>(HttpStatus.OK);
		}
		
		String roomNumber = map.get("roomNumber");
		String nickname = map.get("nickname");
		
		ChatingRoom chatingRoom = findRoom(roomNumber);
		
		if(chatingRoom == null) {
			deleteCookie(); 
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			Map<String, Object> map2 = new HashMap<>();
			map2.put("chatingRoom", chatingRoom);
			map2.put("myNickname", nickname);
			
			return new ResponseEntity<>(map2, HttpStatus.OK);
		}
	}
	
	
	
	//	----------------------------------------------------
	// 메세지 컨트롤러
	
	// 여기서 메세지가 오면 방목록 업데이트
	@MessageMapping("/socket/roomList")
	@SendTo("/topic/roomList")
	public String roomList() {
		return "";
	}
	
	// 채팅방에서 메세지 보내기
	@MessageMapping("/socket/sendMessage/{roomNumber}")
	@SendTo("/topic/message/{roomNumber}")
	public ChatSendVO sendMessage(@DestinationVariable String roomNumber, ChatSendVO chatSendVO) {
		return chatSendVO;
	}
	
	// 채팅방에 입장 퇴장 메세지 보내기
	@MessageMapping("/socket/notification/{roomNumber}")
	@SendTo("/topic/notification/{roomNumber}")
	public Map<String, Object> notification(@DestinationVariable String roomNumber, Map<String, Object> chatingRoom) {
		return chatingRoom;
	}
}