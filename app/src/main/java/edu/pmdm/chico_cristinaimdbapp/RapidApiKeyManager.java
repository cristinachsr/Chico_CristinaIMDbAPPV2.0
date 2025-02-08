package edu.pmdm.chico_cristinaimdbapp;

import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {
    private static final List<String> apiKeys = new ArrayList<>();
    private static int currentKeyIndex = 0;

    static {
        // Añadir tus claves de RapidAPI aquí
        apiKeys.add("1808c2e230msh8489ccd0afda905p11857cjsna37104c15811");//critsinaejemplo
        apiKeys.add("46756a85e9mshff7fcfd2532a075p1d6170jsnf785e47e3f29");// cristinachse28@gmail.com
        apiKeys.add("af5d342519mshdc61bba944f6f16p1e61b7jsn41b61d6d4fe6");//trabajo
        apiKeys.add("6e3a498343msh8500f2f755bdc10p16e1bejsn4a1fa2ed6d02");//cristichico228@gmail.com


    }

    // Devuelve la clave actual
    public static String getApiKey() {
        System.out.println("[claver] 🔑 Clave actual: " + apiKeys.get(currentKeyIndex));
        return apiKeys.get(currentKeyIndex);
    }

    // Rota a la siguiente clave en la lista
    public static void rotateApiKey() {
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size();
        System.out.println("[clave] 🔄 Cambiando a nueva clave API: " + getApiKey());
    }
}
