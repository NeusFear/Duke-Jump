package com.terminalvelocitycabbage.dukejump;

import com.terminalvelocitycabbage.engine.client.ClientBase;
import com.terminalvelocitycabbage.engine.client.input.control.Control;
import com.terminalvelocitycabbage.engine.client.input.control.KeyboardKeyControl;
import com.terminalvelocitycabbage.engine.client.input.controller.BooleanController;
import com.terminalvelocitycabbage.engine.client.input.types.ButtonAction;
import com.terminalvelocitycabbage.engine.client.input.types.KeyboardInput;
import com.terminalvelocitycabbage.engine.client.renderer.Font;
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
import com.terminalvelocitycabbage.engine.client.ui.UIRenderNode;
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
import com.terminalvelocitycabbage.engine.state.State;
import com.terminalvelocitycabbage.engine.util.HeterogeneousMap;
import com.terminalvelocitycabbage.templates.ecs.components.*;
import com.terminalvelocitycabbage.templates.events.*;
import com.terminalvelocitycabbage.templates.meshes.SquareDataMesh;
import org.joml.Vector2f;

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

    //Atlases
    public static Identifier TEXTURE_ATLAS;

    //Textures
    public static Identifier DUKE_TEXTURE;
    public static Identifier GROUND_TEXTURE;
    public static Identifier BUG_TEXTURE;
    public static Identifier BACKGROUND_TEXTURE;

    //Meshes and Models
    public static Identifier SPRITE_MESH;
    public static Identifier DUKE_MODEL;
    public static Identifier GROUND_MODEL;
    public static Identifier BUG_MODEL;
    public static Identifier BACKGROUND_MODEL;

    //Sounds
    public static Identifier SOUND_JUMP_RESOURCE;
    public static Identifier SOUND_JUMP;
    public static Identifier SOUND_DEATH_RESOURCE;
    public static Identifier SOUND_DEATH;

    //Fonts
    public static Identifier PIXEL_FONT_RESOURCE;
    public static Identifier PIXEL_FONT;

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
    public static Identifier BACKGROUND_ENTITY;
    public static Identifier PLAYER_CAMERA_ENTITY;

    //STATES
    public static Identifier SCORE_STATE;
    public static Identifier ALIVE_STATE;

    //Game Configuration
    public static float MOVEMENT_SPEED = -0.8f;
    public static final float GRAVITY = 0.005f;
    public static final float JUMP_FORCE = 1.25f;
    public static final float SCALE = 60f;
    public static final int GROUND_PARTS = 8;
    public static final int GROUND_Y = -100;
    public static final int PLAYER_POSITION_X = -200;
    public static final int BUG_START_POSITION_X = 500;
    public static final float BUG_SPEED_MULTIPLIER = 1.2f;
    public static final int BUG_FREQUENCY = 1000; //duration in ms between bug spawns
    public static final float BACKGROUND_SPEED_MULTIPLIER = 0.2f;
    public static final int BACKGROUND_PARTS = 5;
    public static final float INTERSECTION_RADIUS = SCALE / 2f;

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
            ResourceRegistrationEvent event = (ResourceRegistrationEvent) e;
            //Register texture resources
            DUKE_TEXTURE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "duke.png").getIdentifier();
            GROUND_TEXTURE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "ground.png").getIdentifier();
            BUG_TEXTURE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "bug.png").getIdentifier();
            BACKGROUND_TEXTURE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.TEXTURE, "background.png").getIdentifier();
        });
        getEventDispatcher().listenToEvent(ResourceRegistrationEvent.getEventNameFromCategory(ResourceCategory.SOUND), e -> {
            ResourceRegistrationEvent event = (ResourceRegistrationEvent) e;
            SOUND_JUMP_RESOURCE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.SOUND, "jump.ogg").getIdentifier();
            SOUND_DEATH_RESOURCE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.SOUND, "death.ogg").getIdentifier();
        });
        getEventDispatcher().listenToEvent(SoundRegistrationEvent.EVENT, e -> {
            SoundRegistrationEvent event = (SoundRegistrationEvent) e;
            SOUND_JUMP = event.registerSound(SOUND_JUMP_RESOURCE);
            SOUND_DEATH = event.registerSound(SOUND_DEATH_RESOURCE);
        });
        getEventDispatcher().listenToEvent(ResourceRegistrationEvent.getEventNameFromCategory(ResourceCategory.FONT), e -> {
            ResourceRegistrationEvent event = (ResourceRegistrationEvent) e;
            PIXEL_FONT_RESOURCE = event.registerResource(CLIENT_RESOURCE_SOURCE, ResourceCategory.FONT, "pixel_font.ttf").getIdentifier();
        });
        getEventDispatcher().listenToEvent(FontRegistrationEvent.EVENT, e -> {
            FontRegistrationEvent event = (FontRegistrationEvent) e;
            PIXEL_FONT = event.register(new Font(PIXEL_FONT_RESOURCE)).getIdentifier();
        });
        getEventDispatcher().listenToEvent(ConfigureTexturesEvent.EVENT, e -> {
            ConfigureTexturesEvent event = (ConfigureTexturesEvent) e;
            //Register a default atlas
            TEXTURE_ATLAS = event.registerAtlas(ID, "atlas");
            //Add textures to atlas
            event.addTexture(GROUND_TEXTURE, TEXTURE_ATLAS);
            event.addTexture(DUKE_TEXTURE, TEXTURE_ATLAS);
            event.addTexture(BUG_TEXTURE, TEXTURE_ATLAS);
            event.addTexture(BACKGROUND_TEXTURE, TEXTURE_ATLAS);
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
            BACKGROUND_MODEL = event.registerModel(ID, "background", SPRITE_MESH, BACKGROUND_TEXTURE);
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
            event.registerComponent(BackgroundComponent.class);
            event.registerComponent(SoundSourceComponent.class);
            event.registerComponent(SoundListenerComponent.class);
        });
        getEventDispatcher().listenToEvent(EntitySystemRegistrationEvent.EVENT, e -> {
            EntitySystemRegistrationEvent event = (EntitySystemRegistrationEvent) e;
            event.createSystem(GravitySystem.class);
            event.createSystem(AccelerationSystem.class);
            event.createSystem(UpdateGroundPositionsSystem.class);
            event.createSystem(UpdateBugPositionSystem.class);
            event.createSystem(SpawnBugSystem.class);
            event.createSystem(UpdateBackgroundPositionsSystem.class);
            event.createSystem(CheckForCollisionSystem.class);
            event.createSystem(CountPassedBugsSystem.class);
        });
        getEventDispatcher().listenToEvent(EntityTemplateRegistrationEvent.EVENT, e -> {
            EntityTemplateRegistrationEvent event = (EntityTemplateRegistrationEvent) e;
            DUKE_ENTITY = event.createEntityTemplate(ID, "duke", entity -> {
                entity.addComponent(ModelComponent.class).setModel(DUKE_MODEL);
                entity.addComponent(TransformationComponent.class).setPosition(PLAYER_POSITION_X, GROUND_Y, 0).setScale(SCALE);
                entity.addComponent(VelocityComponent.class);
                entity.addComponent(SoundSourceComponent.class);
                entity.addComponent(SoundListenerComponent.class);
            });
            PLAYER_CAMERA_ENTITY = event.createEntityTemplate(ID, "player_camera", entity -> {
                entity.addComponent(TransformationComponent.class).setPosition(0, 0, -100);
                entity.addComponent(FixedOrthoCameraComponent.class);
            });
            GROUND_ENTITY = event.createEntityTemplate(ID, "ground", entity -> {
                entity.addComponent(ModelComponent.class).setModel(GROUND_MODEL);
                entity.addComponent(TransformationComponent.class).setPosition(-150, GROUND_Y - 150, -5).setScale(SCALE*4f);
                entity.addComponent(GroundComponent.class);
            });
            BUG_ENTITY = event.createEntityTemplate(ID, "bug", entity -> {
                entity.addComponent(ModelComponent.class).setModel(BUG_MODEL);
                entity.addComponent(BugComponent.class);
                entity.addComponent(TransformationComponent.class).setPosition(BUG_START_POSITION_X, GROUND_Y, 5).setScale(SCALE);
            });
            BACKGROUND_ENTITY = event.createEntityTemplate(ID, "background", entity -> {
                entity.addComponent(ModelComponent.class).setModel(BACKGROUND_MODEL);
                entity.addComponent(TransformationComponent.class).setPosition(-300, 80, -10).setScale(SCALE*8f);
                entity.addComponent(BackgroundComponent.class);
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
                    .addStep(event.registerStep(ID, "update_background_positions"), UpdateBackgroundPositionsSystem.class)
                    .addStep(event.registerStep(ID, "check_for_collision"), CheckForCollisionSystem.class)
                    .addStep(event.registerStep(ID, "count_passed_bugs"), CountPassedBugsSystem.class)
                    .build());
        });
        getEventDispatcher().listenToEvent(RendererRegistrationEvent.EVENT, e -> {
            RendererRegistrationEvent event = (RendererRegistrationEvent) e;
            RENDER_GRAPH = event.registerGraph(ID, "render_graph",
                    new RenderGraph(RenderGraph.RenderPath.builder()
                            .addRoutineNode(DEFAULT_ROUTINE)
                            .addRenderNode(event.registerNode(ID, "draw_scene"), DrawSceneRenderNode.class, DEFAULT_SHADER_PROGRAM_CONFIG)
                            .addRenderNode(event.registerNode(ID, "draw_ui"), DRAWUIRenderNode.class, ShaderProgramConfig.EMPTY)
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
        getEventDispatcher().listenToEvent(GameStateRegistrationEvent.EVENT, e -> {
            GameStateRegistrationEvent event = (GameStateRegistrationEvent) e;
            SCORE_STATE = event.registerState(ID, "score", 0);
            ALIVE_STATE = event.registerState(ID, "alive", true);
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

            for (int i = 0; i < BACKGROUND_PARTS; i++) {
                manager.createEntityFromTemplate(BACKGROUND_ENTITY).getComponent(TransformationComponent.class).translate(SCALE*8*i, 0, 0);
            }
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

            if (!(boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue()) return;

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

            boolean alive = (boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue();

            manager.getEntitiesWith(BugComponent.class, TransformationComponent.class).forEach(entity -> {
                var transformation = entity.getComponent(TransformationComponent.class);
                transformation.translate(deltaTime * MOVEMENT_SPEED * BUG_SPEED_MULTIPLIER * (alive ? 1 : 0.2f), 0, 0);
                if (transformation.getPosition().x < (-600)) manager.freeEntity(entity);
            });
        }
    }

    public static class UpdateBackgroundPositionsSystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {

            boolean alive = (boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue();

            manager.getEntitiesWith(BackgroundComponent.class, TransformationComponent.class).forEach(entity -> {
                var transformation = entity.getComponent(TransformationComponent.class);
                transformation.translate(deltaTime * MOVEMENT_SPEED * BACKGROUND_SPEED_MULTIPLIER * (alive ? 1 : 0.1f), 0, 0);
                if (transformation.getPosition().x < (-SCALE - 600)) transformation.translate(SCALE*8*BACKGROUND_PARTS, 0, 0);
            });
        }
    }

    public static class CheckForCollisionSystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {

            var player = manager.getFirstEntityWith(VelocityComponent.class, TransformationComponent.class);
            var transformation = player.getComponent(TransformationComponent.class);
            var playerX = transformation.getPosition().x;
            var playerY = transformation.getPosition().y;

            for (Entity entity : manager.getEntitiesWith(BugComponent.class, TransformationComponent.class)) {
                var entityTransformation = entity.getComponent(TransformationComponent.class);
                var bugX = entityTransformation.getPosition().x;
                var bugY = entityTransformation.getPosition().y;
                if (intersects(playerX, playerY, bugX, bugY)) {
                    DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).setValue(false);
                    player.getComponent(SoundSourceComponent.class).playSound(DukeGameClient.SOUND_DEATH);
                }
            }

        }

        private boolean intersects(float playerX, float playerY, float bugX, float bugY) {
            return Vector2f.distanceSquared(playerX, playerY, bugX, bugY) <= (INTERSECTION_RADIUS * INTERSECTION_RADIUS);
        }
    }

    public static class SpawnBugSystem extends System {

        float duration = 0;

        @Override
        public void update(Manager manager, float deltaTime) {

            if (!(boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue()) return;

            // div by 10 so it doesn't duplicate as much
            if (duration > BUG_FREQUENCY) {
                manager.createEntityFromTemplate(BUG_ENTITY);
                duration -= BUG_FREQUENCY;
            }

            duration += deltaTime;
        }
    }

    public static class CountPassedBugsSystem extends System {

        @Override
        public void update(Manager manager, float deltaTime) {

            if (!(boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue()) return;

            manager.getEntitiesWith(BugComponent.class, TransformationComponent.class).forEach(entity -> {
                if (entity.getComponent(TransformationComponent.class).getPosition().x < (PLAYER_POSITION_X) - INTERSECTION_RADIUS) {
                    if (!entity.getComponent(BugComponent.class).isPassed()) {
                        State<Integer> state = DukeGameClient.getInstance().getStateHandler().getState(SCORE_STATE);
                        state.setValue(state.getValue() + 1);
                    }
                    entity.getComponent(BugComponent.class).pass();
                }
            });
        }
    }

    public static class GroundComponent implements Component {
        @Override
        public void setDefaults() { }
    }

    public static class BugComponent implements Component {

        boolean passed = false;

        @Override
        public void setDefaults() {
            passed = false;
        }

        public boolean isPassed() {
            return passed;
        }

        public void pass() {
            passed = true;
        }
    }

    public static class BackgroundComponent implements Component {
        @Override
        public void setDefaults() { }
    }

    public static class DRAWUIRenderNode extends UIRenderNode {

        public DRAWUIRenderNode(ShaderProgramConfig shaderProgramConfig) {
            super(shaderProgramConfig);
        }

        @Override
        protected Identifier[] getInterestedEvents() {
            return new Identifier[] {};
        }

        @Override
        protected void declareUI() {

            boolean alive = (boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue();

            if (alive) {
                container("float-root z-[10] align-x-[center] align-y-[center] w-[100] h-[30] attach-[top] to-[top] float-offset-y-[30] p-[10]", () -> {
                    text(DukeGameClient.getInstance().getStateHandler().getState(SCORE_STATE).getValue().toString(),
                            "text-size-[40] text-color-[0,0,0,1] font-[" + PIXEL_FONT + "]");
                });
            } else {
                container("border-width-[6] border-color-[0,0,0,1] float-root z-[10] align-x-[center] align-y-[center] w-[360] h-[140] bg-[1,1,1,1] attach-[center] to-[center] p-[10] float-offset-y-[-90]", () -> {
                    text("You Died with a score of: " + DukeGameClient.getInstance().getStateHandler().getState(SCORE_STATE).getValue().toString(),
                            "text-size-[20] text-color-[1,0,0,1] font-[" + PIXEL_FONT + "]");
                });
            }
        }
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
            var transformation = player.getComponent(TransformationComponent.class).getTransformation();
            var shaderProgram = getShaderProgram();

            if (properties.isResized()) {
                camera.updateProjectionMatrix(properties.getWidth(), properties.getHeight());
            }

            shaderProgram.bind();
            shaderProgram.getUniform("textureSampler").setUniform(0);
            shaderProgram.getUniform("projectionMatrix").setUniform(camera.getProjectionMatrix());
            shaderProgram.getUniform("viewMatrix").setUniform(camera.getViewMatrix(transformation));

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

            glClearColor(0f, 0f, 1f, 1.0f);
            shaderProgram.unbind();
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

            if (!(boolean) DukeGameClient.getInstance().getStateHandler().getState(ALIVE_STATE).getValue()) return;

            if (isEnabled()) {
                var manager = ClientBase.getInstance().getManager();
                var player = manager.getFirstEntityWith(TransformationComponent.class, VelocityComponent.class);
                if (player.getComponent(TransformationComponent.class).getPosition().y <= GROUND_Y) {
                    player.getComponent(VelocityComponent.class).setVelocity(0, JUMP_FORCE, 0);
                    player.getComponent(SoundSourceComponent.class).playSound(DukeGameClient.SOUND_JUMP);
                }
            }
        }
    }
}
