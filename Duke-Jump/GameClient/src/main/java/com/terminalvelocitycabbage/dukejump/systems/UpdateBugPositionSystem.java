package com.terminalvelocitycabbage.dukejump.systems;

import com.terminalvelocitycabbage.dukejump.DukeGameClient;
import com.terminalvelocitycabbage.dukejump.components.BugComponent;
import com.terminalvelocitycabbage.dukejump.components.SquashedComponent;
import com.terminalvelocitycabbage.engine.ecs.Manager;
import com.terminalvelocitycabbage.engine.ecs.System;
import com.terminalvelocitycabbage.templates.ecs.components.TransformationComponent;

public class UpdateBugPositionSystem extends System {

    @Override
    public void update(Manager manager, float deltaTime) {

        boolean alive = DukeGameClient.isAlive();

        manager.getEntitiesWith(BugComponent.class, TransformationComponent.class).forEach(entity -> {
            var transformation = entity.getComponent(TransformationComponent.class);
            if (!entity.hasComponent(SquashedComponent.class)) {
                transformation.translate(deltaTime * DukeGameClient.MOVEMENT_SPEED * DukeGameClient.BUG_SPEED_MULTIPLIER * (alive ? 1 : 0.2f), 0, 0);
            } else {
                transformation.rotate(0, 0, deltaTime * 0.6f);
            }
            if (transformation.getPosition().x < -600 || transformation.getPosition().y < -600)
                manager.freeEntity(entity);
        });
    }
}
