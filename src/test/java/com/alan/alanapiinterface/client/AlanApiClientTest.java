package com.alan.alanapiinterface.client;

import com.alan.alanapiinterface.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AlanApiClientTest
 *
 * @author ALan
 * @date 2026/8/30 16:24
 *
 */
class AlanApiClientTest {

    @Test
    void getNameByGet() {
        AlanApiClient alanApiClient = new AlanApiClient();
        String nameByGet = alanApiClient.getNameByGet("alan");
        System.out.println(nameByGet);
    }

    @Test
    void getNameByPost() {
        AlanApiClient alanApiClient = new AlanApiClient();
        String nameByGet = alanApiClient.getNameByPost("alan");
        System.out.println(nameByGet);
    }

    @Test
    void getUsernameByPost() {
        AlanApiClient alanApiClient = new AlanApiClient();
        String nameByGet = alanApiClient.getUsernameByPost(new User("alan"));
        System.out.println(nameByGet);
    }

}