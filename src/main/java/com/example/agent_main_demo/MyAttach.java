package com.example.agent_main_demo;

import java.io.IOException;
import java.util.Scanner;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class MyAttach {
    public static void main(String[] args)
            throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        Scanner sc = new Scanner(System.in);
        String pid = sc.nextLine().trim();
        sc.close();

        VirtualMachine vm = VirtualMachine.attach(pid);

        String agentJarPath = "C:\\java_project\\15-hook-jeb-jar\\target\\agent-main-demo.jar";
        String agentArgs = "agentArgHello";
        vm.loadAgent(agentJarPath, agentArgs);
        vm.detach();
    }
}
