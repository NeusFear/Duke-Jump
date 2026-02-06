package com.terminalvelocitycabbage.dukejump;

import com.terminalvelocitycabbage.engine.client.ClientBase;
import com.terminalvelocitycabbage.engine.client.input.control.Control;
import com.terminalvelocitycabbage.engine.client.input.control.KeyboardKeyControl;
import com.terminalvelocitycabbage.engine.client.input.controller.BooleanController;
import com.terminalvelocitycabbage.engine.client.input.types.ButtonAction;
import com.terminalvelocitycabbage.engine.client.input.types.KeyboardInput;
import com.terminalvelocitycabbage.engine.client.renderer.RenderGraph;
import com.terminalvelocitycabbage.engine.client.renderer.elements.VertexAttribute;
import com.terminalvelocitycabbage.engine.client.renderer.elements.VertexFormat;
import com.terminalvelocitycabbage.engine.client.renderer.model.Mesh;
import com.terminalvelocitycabbage.engine.client.renderer.model.MeshCache;
import com.terminalvelocitycabbage.engine.client.renderer.model.Model;
import com.terminalvelocitycabbage.engine.client.renderer.shader.Shader;
import com.terminalvelocitycabbage.engine.client.renderer.shader.ShaderProgramConfig;
import com.terminalvelocitycabbage.engine.client.renderer.shader.Uniform;
import com.terminalvelocitycabbage.engine.client.scene.Scene;
import com.terminalvelocitycabbage.engine.client.window.WindowProperties;
import com.terminalvelocitycabbage.engine.debug.Log;
import com.terminalvelocitycabbage.engine.ecs.Component;
import com.terminalvelocitycabbage.engine.ecs.Entity;
import com.terminalvelocitycabbage.engine.ecs.Manager;
import com.terminalvelocitycabbage.engine.ecs.System;
import com.terminalvelocitycabbage.engine.filesystem.resources.ResourceCategory;
import com.terminalvelocitycabbage.engine.filesystem.resources.ResourceSource;
import com.terminalvelocitycabbage.engine.filesystem.sources.MainSource;
import com.terminalvelocitycabbage.engine.graph.RenderNode;
import com.terminalvelocitycabbage.engine.graph.Routine;
import com.terminalvelocitycabbage.engine.registry.Identifier;
import com.terminalvelocitycabbage.engine.util.HeterogeneousMap;
import com.terminalvelocitycabbage.templates.ecs.components.*;
import com.terminalvelocitycabbage.templates.events.*;
import com.terminalvelocitycabbage.templates.meshes.SquareDataMesh;

import java.util.List;

import static org.lwjgl.opengl.GL11C.glClearColor;

public class DukeGameClient extends ClientBase {

    //This client's identifier (namespace)
    public static final String ID = "dukejump";

    //Resource stuff
    public static Identifier CLIENT_RESOURCE_SOURCE;

    //Shader stuff
    public static Identifier DEFAULT_VERTEX_SHADER;
    public static Identifier DEFAULT_FRAGMENT_SHADER;
    public static ShaderProgramConfig DEFAULT_SHADER_PROGRAM_CONFIG;

    //Textures and atlases
    public static Identifier DUKE_TEXTURE;
    public static Identifier GROUND_TEXTURE;
    public static Identifier TEXTURE_ATLAS;
    public static Identifier BUG_TEXTURE;

    //Meshes and Models
    public static Identifier SPRITE_MESH;
    public static Identifier DUKE_MODEL;
    public static Identifier GROUND_MODEL;
    public static Identifier BUG_MODEL;

    //Renderer configs
    public static final VertexFormat MESH_FORMAT = VertexFormat.builder()
            .addElement(VertexAttribute.XYZ_POSITION)
            .addElement(VertexAttribute.UV)
            .build();
    public static Routine DEFAULT_ROUTINE;
    public static Identifier RENDER_GRAPH;
    private static Identifier DEFAULT_SCENE;

    //Entity stuff
    public static Identifier DUKE_ENTITY;
    public static Identifier GROUND_ENTITY;
    public static Identifier BUG_ENTITY;
    public static Identifier PLAYER_CAMERA_ENTITY;

    //Game Configuration
    public static final float MOVEMENT_SPEED = -0.8f;
    public static final float GRAVITY = 0.005f;
    public static final float JUMP_FORCE = 1.25f;
    public static final float SCALE = 60f;
    public static final int GROUND_PARTS = 8;
    public static final int GROUND_Y = -100;
    public static final int PLAYER_POSITION_X = -200;
    public static final int BUG_START_POSITION_X = 500;
    public static final float BUG_SPEED_MULTIPLIER = 1.2f;
    public static final int BUG_FREQUENCY = 1000; //duration in ms between bug spawns

    public DukeGameClient(String namespace, int ticksPerSecond) {
        super(namespace, ticksPerSecond);
        //Listen to events
        getEventDispatcher().listenToEvent(ResourceCategoryRegistrationEvent.EVENT, e -> {
            //Register engine defaults
            ResourceCategory.registerEngineDefaults(((ResourceCategoryRegistrationEvent) e).getRegistry(), ID);
        });
        getEventDispatcher().listenToEvent(ResourceSourceRegistrationEvent.EVENT, e -> {
            //Register and init filesystem things
            //Create resource sources for this client
            ResourceSource mainSource = new MainSource(getInstance());
            //Define roots for these resources based on default resoruce categories
            mainSource.registerDefaultSources(ID);
            //register this source
            CLIENT_RESOURCE_SOURCE = ((ResourceSourceRegistrationEvent) e).registerResourceSource(ID, "main", mainSource);
        });
        getEventDispatcher().listenToEvent(ResourceRegistrationEvent.getEventNameFromCategory(ResourceCategory.SHADER), e -> {
            ResourceRegistrationEvent event = (ResourceRegistrationEvent) e;
            //Register shader resources
            DEFAULT_VERTEX_SHADER = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.SHADER, "default.vert").getIdentifier();
            DEFAULT_FRAGMENT_SHADER = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.SHADER, "default.frag").getIdentifier();
            //Configure the shader program
            DEFAULT_SHADER_PROGRAM_CONFIG = ShaderProgramConfig.builder()
                    .vertexFormat(MESH_FORMAT)
                    .addShader(Shader.Type.VERTEX, DEFAULT_VERTEX_SHADER)
                    .addShader(Shader.Type.FRAGMENT, DEFAULT_FRAGMENT_SHADER)
                    .addUniform(new Uniform("textureSampler"))
                    .addUniform(new Uniform("projectionMatrix"))
                    .addUniform(new Uniform("viewMatrix"))
                    .addUniform(new Uniform("modelMatrix"))
                    .build();
        });
        getEventDispatcher().listenToEvent(ResourceRegistrationEvent.getEventNameFromCategory(ResourceCategory.TEXTURE), e -> {
            //Register texture resources
            DUKE_TEXTURE = ((ResourceRegistrationEvent) e).registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "duke.png").getIdentifier();
            GROUND_TEXTURE = ((ResourceRegistrationEvent) e).registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "ground.png").getIdentifier();
            BUG_TEXTURE = ((ResourceRegistrationEvent) e).registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "bug.png").getIdentifier();
        });
        getEventDispatcher().listenToEvent(ConfigureTexturesEvent.EVENT, e -> {
            ConfigureTexturesEvent event = (ConfigureTexturesEvent) e;
            //Register a default atlas
            TEXTURE_ATLAS = event.registerAtlas(ID, "atlas");
            //Add textures to atlas
            event.addTexture(GROUND_TEXTURE, TEXTURE_ATLAS);
            event.addTexture(DUKE_TEXTURE, TEXTURE_ATLAS);
            event.addTexture(BUG_TEXTURE, TEXTURE_ATLAS);
        });
        getEventDispatcher().listenToEvent(MeshRegistrationEvent.EVENT, e -> {
            MeshRegistrationEvent event = (MeshRegistrationEvent) e;
            SPRITE_MESH = event.registerMesh(ID, "sprite", new Mesh(MESH_FORMAT, new SquareDataMesh()));
        });
        getEventDispatcher().listenToEvent(ModelConfigRegistrationEvent.EVENT, e -> {
            ModelConfigRegistrationEvent event = (ModelConfigRegistrationEvent) e;
            DUKE_MODEL = event.registerModel(ID, "duke", SPRITE_MESH, DUKE_TEXTURE);
            GROUND_MODEL = event.registerModel(ID, "ground", SPRITE_MESH, GROUND_TEXTURE);
            BUG_MODEL = event.registerModel(ID, "bug", SPRITE_MESH, BUG_TEXTURE);
        });
        getEventDispatcher().listenToEvent(EntityComponentRegistrationEvent.EVENT, e -> {
            EntityComponentRegistrationEvent event = (EntityComponentRegistrationEvent) e;
            event.registerComponent(ModelComponent.class);
            event.registerComponent(TransformationComponent.class);
            event.registerComponent(PositionComponent.class);
            event.registerComponent(FixedOrthoCameraComponent.class);
            event.registerComponent(VelocityComponent.class);
            event.registerComponent(GroundComponent.class);
            event.registerComponent(BugComponent.class);
        });
        getEventDispatcher().listenToEvent(EntitySystemRegistrationEvent.EVENT, e -> {
            EntitySystemRegistrationEvent event = (EntitySystemRegistrationEvent) e;
            event.createSystem(GravitySystem.class);
            event.createSystem(AccelerationSystem.class);
            event.createSystem(UpdateGroundPositionsSystem.class);
            event.createSystem(UpdateBugPositionSystem.class);
            event.createSystem(SpawnBugSystem.class);
        });
        getEventDispatcher().listenToEvent(EntityTemplateRegistrationEvent.EVENT, e -> {
            EntityTemplateRegistrationEvent event = (EntityTemplateRegistrationEvent) e;
            DUKE_ENTITY = event.createEntityTemplate(ID, "duke", entity -> {
                entity.addComponent(ModelComponent.class).setModel(DUKE_MODEL);
                entity.addComponent(TransformationComponent.class).setPosition(PLAYER_POSITION_X, GROUND_Y, 0).setScale(SCALE);
                entity.addComponent(VelocityComponent.class);
            });
            PLAYER_CAMERA_ENTITY = event.createEntityTemplate(ID, "player_camera", entity -> {
                entity.addComponent(PositionComponent.class).setPosition(0, 0, -100);
                entity.addComponent(FixedOrthoCameraComponent.class);
            });
            GROUND_ENTITY = event.createEntityTemplate(ID, "ground", entity -> {
                entity.addComponent(ModelComponent.class).setModel(GROUND_MODEL);
                entity.addComponent(TransformationComponent.class).setPosition(0, GROUND_Y - 150, 0).setScale(SCALE*4f);
                entity.addComponent(GroundComponent.class);
            });
            BUG_ENTITY = event.createEntityTemplate(ID, "bug", entity -> {
                entity.addComponent(ModelComponent.class).setModel(BUG_MODEL);
                entity.addComponent(BugComponent.class);
                entity.addComponent(TransformationComponent.class).setPosition(BUG_START_POSITION_X, GROUND_Y, 0).setScale(SCALE);
            });
        });
        getEventDispatcher().listenToEvent(RoutineRegistrationEvent.EVENT, e -> {
            RoutineRegistrationEvent event = (RoutineRegistrationEvent) e;
            DEFAULT_ROUTINE = event.registerRoutine(Routine.builder(ID, "update_duke_positions")
                    .addStep(event.registerStep(ID, "gravity"), GravitySystem.class)
                    .addStep(event.registerStep(ID, "acceleration"), AccelerationSystem.class)
                    .addStep(event.registerStep(ID, "update_ground_positions"), UpdateGroundPositionsSystem.class)
                    .addStep(event.registerStep(ID, "update_bug_positions"), UpdateBugPositionSystem.class)
                    .addStep(event.registerStep(ID, "spawn_bug"), SpawnBugSystem.class)
                    .build());
            Log.info(DEFAULT_ROUTINE);
        });
        getEventDispatcher().listenToEvent(RendererRegistrationEvent.EVENT, e -> {
            RendererRegistrationEvent event = (RendererRegistrationEvent) e;
            RENDER_GRAPH = event.registerGraph(ID, "render_graph",
                    new RenderGraph(RenderGraph.RenderPath.builder()
                            .addRoutineNode(DEFAULT_ROUTINE)
                            .addRenderNode(event.registerNode(ID, "draw_scene"), DrawSceneRenderNode.class, DEFAULT_SHADER_PROGRAM_CONFIG)
                    )
            );
        });
        getEventDispatcher().listenToEvent(SceneRegistrationEvent.EVENT, e -> {
            SceneRegistrationEvent event = (SceneRegistrationEvent) e;
            DEFAULT_SCENE = event.registerScene(ID, "scene", new DefaultScene(RENDER_GRAPH, List.of()));
        });
        getEventDispatcher().listenToEvent(InputHandlerRegistrationEvent.EVENT, e -> {
            InputHandlerRegistrationEvent event = (InputHandlerRegistrationEvent) e;

            var inputHandler = event.getInputHandler();
            //Register Controls
            Control exitControl = inputHandler.registerControlListener(new KeyboardKeyControl(KeyboardInput.Key.ESCAPE));
            Control jumpControl = inputHandler.registerControlListener(new KeyboardKeyControl(KeyboardInput.Key.SPACE));
            //Register Controllers
            inputHandler.registerController(ID, "exit_game", new CloseGameController(exitControl));
            inputHandler.registerController(ID, "jump", new JumpController(jumpControl));
        });
    }

    public static void main(String[] args) {
        DukeGameClient client = new DukeGameClient(ID, 60);
        client.start();
    }

    @Override
    public void init() {
        super.init();

        //Create window properties
        WindowProperties windowProperties = new WindowProperties(800, 600, "Duke Jump Game", DEFAULT_SCENE);
        //Create window
        long window = getWindowManager().createNewWindow(windowProperties);
        //Focus window
        getWindowManager().focusWindow(window);
    }

    public static class DefaultScene extends Scene {

        public DefaultScene(Identifier renderGraph, List<Routine> routines) {
            super(renderGraph, routines);
        }

        @Override
        public void init() {
            var client = DukeGameClient.getInstance();
            var manager = client.getManager();

            client.getTextureCache().generateAtlas(TEXTURE_ATLAS);
            setMeshCache(new MeshCache(client.getModelRegistry(), client.getMeshRegistry(), client.getTextureCache()));

            manager.createEntityFromTemplate(DUKE_ENTITY);
            manager.createEntityFromTemplate(PLAYER_CAMERA_ENTITY);
            for (int i = 0; i < GROUND_PARTS; i++) {
                manager.createEntityFromTemplate(GROUND_ENTITY).getComponent(TransformationComponent.class).translate(SCALE*4*i, 0, 0);
            }
        }

        @Override
        public void cleanup() {
            var client = DukeGameClient.getInstance();
            client.getTextureCache().cleanupAtlas(TEXTURE_ATLAS);
            getMeshCache().cleanup();
        }
    }

    public static class GravitySystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {
            manager.getEntitiesWith(VelocityComponent.class).forEach(entity -> {
                if (entity.getComponent(TransformationComponent.class).getPosition().y < GROUND_Y) {
                    entity.getComponent(VelocityComponent.class).setVelocity(0, 0, 0);
                    entity.getComponent(TransformationComponent.class).setPosition(PLAYER_POSITION_X, GROUND_Y, 0);
                } else {
                    entity.getComponent(VelocityComponent.class).addVelocity(0, -GRAVITY * deltaTime, 0);
                }
            });
        }
    }

    public static class AccelerationSystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {
            manager.getEntitiesWith(VelocityComponent.class, TransformationComponent.class).forEach(entity -> {
                var velocity = entity.getComponent(VelocityComponent.class).getVelocity();
                var transformationComponent = entity.getComponent(TransformationComponent.class);
                transformationComponent.translate(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
                if (transformationComponent.getPosition().y < GROUND_Y) transformationComponent.setPosition(transformationComponent.getPosition().x, GROUND_Y, transformationComponent.getPosition().z);
            });
        }
    }

    public static class UpdateGroundPositionsSystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {
            manager.getEntitiesWith(GroundComponent.class, TransformationComponent.class).forEach(entity -> {
                var transformation = entity.getComponent(TransformationComponent.class);
                transformation.translate(deltaTime * MOVEMENT_SPEED, 0, 0);
                if (transformation.getPosition().x < (-SCALE - 600)) transformation.translate(SCALE*4*GROUND_PARTS, 0, 0);
            });
        }
    }

    public static class UpdateBugPositionSystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {
            manager.getEntitiesWith(BugComponent.class, TransformationComponent.class).forEach(entity -> {
                var transformation = entity.getComponent(TransformationComponent.class);
                transformation.translate(deltaTime * MOVEMENT_SPEED * BUG_SPEED_MULTIPLIER, 0, 0);
                if (transformation.getPosition().x < (-600)) manager.freeEntity(entity);
            });
        }
    }

    public static class SpawnBugSystem extends System {

        float duration = 0;

        @Override
        public void update(Manager manager, float deltaTime) {

            // div by 10 so it doesn't duplicate as much
            if (duration > BUG_FREQUENCY) {
                manager.createEntityFromTemplate(BUG_ENTITY);
                duration -= BUG_FREQUENCY;
                Log.info("Spawned bug");
            }

            duration += deltaTime;
        }
    }

    public static class GroundComponent implements Component {
        @Override
        public void setDefaults() { }
    }

    public static class BugComponent implements Component {
        @Override
        public void setDefaults() { }
    }

    public static class DrawSceneRenderNode extends RenderNode {

        public DrawSceneRenderNode(ShaderProgramConfig shaderProgramConfig) {
            super(shaderProgramConfig);
        }

        @Override
        public void execute(Scene scene, WindowProperties properties, HeterogeneousMap renderConfig, long deltaTime) {

            var client = DukeGameClient.getInstance();
            var player = client.getManager().getFirstEntityWith(FixedOrthoCameraComponent.class);
            var camera = player.getComponent(FixedOrthoCameraComponent.class);
            var shaderProgram = getShaderProgram();

            if (properties.isResized()) {
                camera.updateProjectionMatrix(properties.getWidth(), properties.getHeight());
            }

            shaderProgram.bind();
            shaderProgram.getUniform("textureSampler").setUniform(0);
            shaderProgram.getUniform("projectionMatrix").setUniform(camera.getProjectionMatrix());
            shaderProgram.getUniform("viewMatrix").setUniform(camera.getViewMatrix(player));

            var entities = client.getManager().getEntitiesWith(ModelComponent.class, TransformationComponent.class);

            //Render entities
            for (Entity entity : entities) {
                var modelIdentifier = entity.getComponent(ModelComponent.class).getModel();
                var model = client.getModelRegistry().get(modelIdentifier);
                var mesh = scene.getMeshCache().getMesh(modelIdentifier);
                var texture = client.getTextureCache().getTexture(model.getTextureIdentifier());
                var transformationComponent = entity.getComponent(TransformationComponent.class);

                texture.bind();
                shaderProgram.getUniform("modelMatrix").setUniform(transformationComponent.getTransformationMatrix());
                if (mesh.getFormat().equals(shaderProgram.getConfig().getVertexFormat())) mesh.render();
            }

            shaderProgram.unbind();

            //TODO remove this because we don't really need it, just don't have a background yet
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        }
    }

    public static class CloseGameController extends BooleanController {

        public CloseGameController(Control... controls) {
            super(ButtonAction.PRESSED, false, controls);
        }

        @Override
        public void act() {
            if (isEnabled()) DukeGameClient.getInstance().getWindowManager().closeFocusedWindow();
        }
    }

    public static class JumpController extends BooleanController {

        public JumpController(Control... controls) {
            super(ButtonAction.PRESSED, false, controls);
        }

        @Override
        public void act() {
            if (isEnabled()) {
                var manager = ClientBase.getInstance().getManager();
                manager.getEntitiesWith(TransformationComponent.class, VelocityComponent.class).forEach(entity -> {
                    if (entity.getComponent(TransformationComponent.class).getPosition().y <= GROUND_Y) {
                        entity.getComponent(VelocityComponent.class).setVelocity(0, JUMP_FORCE, 0);
                    }
                });
            }
        }
    }
}
