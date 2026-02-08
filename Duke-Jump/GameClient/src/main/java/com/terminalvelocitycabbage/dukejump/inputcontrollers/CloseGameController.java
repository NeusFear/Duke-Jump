package com.terminalvelocitycabbage.dukejump.inputcontrollers;

import com.terminalvelocitycabbage.dukejump.DukeGameClient;
import com.terminalvelocitycabbage.engine.client.input.control.Control;
import com.terminalvelocitycabbage.engine.client.input.controller.BooleanController;
import com.terminalvelocitycabbage.engine.client.input.types.ButtonAction;

public class CloseGameController extends BooleanController {

    public CloseGameController(Control... controls) {
        super(ButtonAction.PRESSED, false, controls);
    }

    @Override
    public void act() {
        if (isEnabled()) DukeGameClient.getInstance().getWindowManager().closeFocusedWindow();
    }
}
