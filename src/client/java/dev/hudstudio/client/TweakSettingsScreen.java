package dev.hudstudio.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class TweakSettingsScreen extends StudioScreen {
    private final HudConfig.Element setting;
    private boolean binding;
    public TweakSettingsScreen(Screen parent,HudConfig.Element setting) {super(HudLayout.name(setting),parent);this.setting=setting;}
    @Override protected void init() {
        clearWidgets();int x=width/2-120;
        addRenderableWidget(new StudioUi.Action(x,52,240,22,"Enabled: "+(setting.visible?"ON":"OFF"),()->{
            setting.visible=!setting.visible;HudStudioClient.config.save();init();
        }));
        var key=ClientTweaks.key(setting.id);
        if(key!=null)addRenderableWidget(new StudioUi.Action(x,82,240,22,binding?"Press a key or mouse button...":"Key: "+key.getTranslatedKeyMessage().getString(),()->{binding=true;init();}));
        if(setting.id.equals("zoom") || setting.id.equals("freecam"))addRenderableWidget(new ValueSlider(x,116,setting.id.equals("zoom")));
        addRenderableWidget(new StudioUi.Action(width/2-36,height-25,72,18,"Back",this::onClose));
    }
    private void bind(InputConstants.Key key) {
        ClientTweaks.key(setting.id).setKey(key);KeyMapping.resetMapping();minecraft.options.save();binding=false;init();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(binding) {if(!event.isEscape())bind(InputConstants.Type.KEYBOARD.getOrCreate(event.key()));else {binding=false;init();}return true;}
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(binding) {bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));return true;}
        return super.mouseClicked(event,twice);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        StudioUi.centered(g,HudLayout.name(setting),width/2f,17,width-24,1,StudioUi.TEXT);
        String[] lines=switch(setting.id) {
            case "zoom"->new String[]{"Hold your key for a closer view.","Mouse sensitivity follows the zoom level."};
            case "freecam"->new String[]{"Press the key to enter or leave freecam.","WASD moves the camera; jump / sneak moves up / down.","Hold sprint for a speed boost. Your body stays behind.","Explore loaded chunks; attacking and using items are blocked."};
            case "freelook"->new String[]{"Hold the key to orbit in third person.","Your character keeps its aim and movement direction.","Release to restore your previous camera view."};
            case "no_shake"->new String[]{"Removes walking camera bob and damage tilt.","Your normal Minecraft settings stay unchanged."};
            default->new String[]{"Hides explosion bursts, emitters and block debris.","Explosion sounds and gameplay remain unchanged."};
        };
        int y=setting.id.equals("zoom") || setting.id.equals("freecam")?158:120;
        for(String line:lines) {StudioUi.centered(g,line,width/2f,y,width-28,.72f,StudioUi.MUTED);y+=13;}
        super.extractRenderState(g,mx,my,a);
    }
    private final class ValueSlider extends AbstractSliderButton {
        private final boolean zoom;
        ValueSlider(int x,int y,boolean zoom) {super(x,y,240,22,Component.empty(),zoom?(HudStudioClient.config.zoomFactor-2)/10:(HudStudioClient.config.freecamSpeed-.1)/2.9);this.zoom=zoom;updateMessage();}
        @Override protected void updateMessage() {setMessage(Component.literal(zoom?String.format(java.util.Locale.ROOT,"Zoom: %.1fx",HudStudioClient.config.zoomFactor):String.format(java.util.Locale.ROOT,"Camera speed: %.1f blocks / tick",HudStudioClient.config.freecamSpeed)));}
        @Override protected void applyValue() {if(zoom)HudStudioClient.config.zoomFactor=(float)(2+value*10);else HudStudioClient.config.freecamSpeed=(float)(.1+value*2.9);HudStudioClient.config.save();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
            StudioUi.card(g,getX(),getY(),width,height,StudioUi.PANEL);
            StudioUi.box(g,getX(),getY()+height-3,(int)(width*value),3,0xffaaaaaa);
            StudioUi.centered(g,getMessage().getString(),getX()+width/2f,getY()+7,width-10,.8f,StudioUi.TEXT);handleCursor(g);
        }
    }
}
