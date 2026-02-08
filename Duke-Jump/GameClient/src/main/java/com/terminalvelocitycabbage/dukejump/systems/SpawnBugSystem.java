package com.terminalvelocitycabbage.dukejump.systems;

import com.terminalvelocitycabbage.dukejump.DukeGameClient;
import com.terminalvelocitycabbage.engine.ecs.Manager;
import com.terminalvelocitycabbage.engine.ecs.System;

public class SpawnBugSystem extends System {

    float duration = 0;
    int variation = 0;

    @Override
    public void update(Manager manager, float deltaTime) {

        if (!(boolean) DukeGameClient.isAlive()) return;

        if (duration > (DukeGameClient.BUG_FREQUENCY + variation)) {
            manager.createEntityFromTemplate(DukeGameClient.BUG_ENTITY);
            duration -= DukeGameClient.BUG_FREQUENCY;
            variation = (int) (Math.random() * DukeGameClient.BUG_FREQUENCY_VARIANCE);
        }

        duration += deltaTime;
    }
}
