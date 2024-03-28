package ca.uoit.websocketshelloworld;

import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ServerEndpoint(value="/ws/{roomId}")
public class ChatServer {
    private Map<String, String> usernames = new HashMap<String, String>();
    private static Map<String, String> roomList = new HashMap<String, String>();

    @OnOpen
    public void open(@PathParam("roomId")String roomId, Session session) throws IOException{
        //System.out.println("Client Connected: " + session.getId());
        roomList.put(session.getId(), roomId);
        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome to the chat room. Please state your username to begin.\"}");
    }

    @OnClose
    public void close(@PathParam("roomId")String roomId,Session session) throws IOException{
        String userId = session.getId();
        if (usernames.containsKey(userId)) {
            String username = usernames.get(userId);
            usernames.remove(userId);
            //broadcast this person left the server
            for (Session peer : session.getOpenSessions()){
                if (roomList.get(peer.getId()).equals(roomId))
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + username + " left the chat room.\"}");
            }
        }
    }

    @OnMessage
    public void handleMessage(@PathParam("roomId")String roomId,String comm, Session session) throws IOException{
        String userID = session.getId();
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        // not their first message
        if(usernames.containsKey(userID)){
            String username = usernames.get(userID);
            System.out.println(username);
            for(Session peer: session.getOpenSessions()){
                if (roomList.get(peer.getId()).equals(roomId))
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message+"\"}");
            }
        }else{ //first message is their username
            usernames.put(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome, " + message + "!\"}");
            //broadcast this person joined the server to the rest
            for(Session peer: session.getOpenSessions()){
                if(!peer.getId().equals(userID) && roomList.get(peer.getId()).equals(roomId)){
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " joined the chat room.\"}");
                }
            }
        }

    }

}