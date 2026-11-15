package dev.hudstudio.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.function.*;

public final class TweakSettingsScreen extends StudioScreen {
    private final HudConfig.Element setting;
    private boolean binding;
    private int page,row,x;
    public TweakSettingsScreen(Screen parent,HudConfig.Element setting) {super(HudLayout.name(setting),parent);this.setting=setting;}
    @Override protected void init() {
        clearWidgets();x=width/2-120;row=40;
        var c=HudStudioClient.config;
        if(setting.id.equals("freecam")) {
            String[] pages={"Controls","Movement","Display"};
            for(int i=0;i<pages.length;i++) {
                final int index=i;
                addRenderableWidget(new StudioUi.Action(x+i*81,32,78,20,(page==i?"> ":"")+pages[i],()->{page=index;binding=false;init();}));
            }
            row=62;
        }
        if(!setting.id.equals("freecam") || page==0) {
            toggle("Enabled",()->setting.visible,v->setting.visible=v);
            var key=ClientTweaks.key(setting.id);
            if(key!=null)action(binding?"Press a key or mouse button...":"Key: "+key.getTranslatedKeyMessage().getString(),()->{binding=true;init();});
        }
        switch(setting.id) {
            case "zoom" -> {
                slider("Starting zoom",1,50,()->c.zoomFactor,v->c.zoomFactor=(float)v,"x");
                toggle("Show HUD",()->c.zoomShowHud,v->c.zoomShowHud=v);
                toggle("Cinematic zoom",()->c.zoomCinematic,v->c.zoomCinematic=v);
            }
            case "freecam" -> {
                if(page==0) {
                    action("Activation: "+(c.freecamHold?"Hold":"Toggle"),()->{c.freecamHold=!c.freecamHold;save();});
                    toggle("Exit on damage",()->c.freecamDisableOnDamage,v->c.freecamDisableOnDamage=v);
                } else if(page==1) {
                    action("Flight: "+(c.freecamLookFlight?"Follow crosshair":"Horizontal"),()->{c.freecamLookFlight=!c.freecamLookFlight;save();});
                    slider("Horizontal speed",.1,3,()->c.freecamSpeed,v->c.freecamSpeed=(float)v," blocks/tick");
                    slider("Vertical speed",.1,3,()->c.freecamVerticalSpeed,v->c.freecamVerticalSpeed=(float)v," blocks/tick");
                    slider("Sprint boost",1,5,()->c.freecamSprintMultiplier,v->c.freecamSprintMultiplier=(float)v,"x");
                    toggle("Block collision",()->c.freecamCollision,v->c.freecamCollision=v);
                } else {
                    toggle("Show player",()->c.freecamShowPlayer,v->c.freecamShowPlayer=v);
                    toggle("Show HUD",()->c.freecamShowHud,v->c.freecamShowHud=v);
                    toggle("Submersion fog",()->c.freecamShowSubmersion,v->c.freecamShowSubmersion=v);
                }
            }
        }
        addRenderableWidget(new StudioUi.Action(width/2-36,height-25,72,18,"Back",this::onClose));
    }
    private void action(String label,Runnable action) {addRenderableWidget(new StudioUi.Action(x,row,240,22,label,action));row+=26;}
    private void save() {HudStudioClient.config.save();init();}
    private void toggle(String label,BooleanSupplier get,Consumer<Boolean> set) {action(label+": "+(get.getAsBoolean()?"ON":"OFF"),()->{set.accept(!get.getAsBoolean());save();});}
    private void slider(String label,double min,double max,DoubleSupplier get,DoubleConsumer set,String suffix) {addRenderableWidget(new ValueSlider(x,row,label,min,max,get,set,suffix));row+=26;}
    private void bind(InputConstants.Key key) {ClientTweaks.key(setting.id).setKey(key);KeyMapping.resetMapping();minecraft.options.save();binding=false;init();}
    @Override public boolean keyPressed(KeyEvent event) {
        if(binding) {if(!event.isEscape())bind(InputConstants.Type.KEYBOARD.getOrCreate(event.key()));else {binding=false;init();}return true;}
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(binding) {bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));return true;}
        return super.mouseClicked(event,twice);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        StudioUi.centered(g,HudLayout.name(setting),width/2f,13,width-24,1,StudioUi.TEXT);
        if(!setting.id.equals("zoom") && !setting.id.equals("freecam")) {
            String hint=switch(setting.id) {
                case "freelook"->"Hold to orbit. Release to restore your view.";
                case "no_shake"->"Removes walking bob and damage tilt.";
                case "full_bright"->"Maximum gamma while enabled. Saved brightness is preserved.";
                default->"Hides explosion particles; keeps sounds.";
            };
            StudioUi.centered(g,hint,width/2f,row+10,width-28,.72f,StudioUi.MUTED);
        }
        super.extractRenderState(g,mx,my,a);
    }
    private final class ValueSlider extends AbstractSliderButton {
        private final String label,suffix;
        private final double min,max;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        ValueSlider(int x,int y,String label,double min,double max,DoubleSupplier get,DoubleConsumer set,String suffix) {
            super(x,y,240,22,Component.empty(),(get.getAsDouble()-min)/(max-min));
            this.label=label;this.min=min;this.max=max;this.get=get;this.set=set;this.suffix=suffix;updateMessage();
        }
        @Override protected void updateMessage() {setMessage(Component.literal(label+": "+String.format(java.util.Locale.ROOT,"%.1f",get.getAsDouble())+suffix));}
        @Override protected void applyValue() {set.accept(min+value*(max-min));HudStudioClient.config.save();}
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
            StudioUi.card(g,getX(),getY(),width,height,StudioUi.PANEL);
            StudioUi.box(g,getX(),getY()+height-3,(int)(width*value),3,StudioTheme.accent());
            StudioUi.centered(g,getMessage().getString(),getX()+width/2f,getY()+7,width-10,.8f,StudioUi.TEXT);handleCursor(g);
        }
    }
}
