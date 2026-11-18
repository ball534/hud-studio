package dev.hudstudio.client.navigation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hudstudio.HudStudio;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class WaypointStore {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public final Path directory;
    public final String label;
    private List<Waypoint> entries=new ArrayList<>();
    private boolean readable=true;
    private static class Data {int schema=1;List<Waypoint> waypoints=new ArrayList<>();}

    public WaypointStore(Path directory,String label) {
        this.directory=directory;this.label=label;
        var file=directory.resolve("waypoints.json");
        if(Files.exists(file))try {
            var data=JSON.fromJson(Files.readString(file),Data.class);
            if(data==null || data.waypoints==null)throw new IOException("Invalid waypoint file");
            Set<String> ids=new HashSet<>();
            for(var w:data.waypoints) {
                if(w==null || w.id()==null || w.name()==null || w.dimension()==null || !ids.add(w.id())
                    || !validCoordinate(w.x()) || !validCoordinate(w.y()) || !validCoordinate(w.z()))throw new IOException("Invalid waypoint entry");
                entries.add(w);
            }
        }catch(Exception e) {readable=false;entries.clear();HudStudio.LOGGER.error("Cannot read waypoints at {}",file,e);}
    }
    public static boolean validCoordinate(int value) {return value>=-30_000_000 && value<=30_000_000;}
    public boolean readable() {return readable;}
    public List<Waypoint> all() {return List.copyOf(entries);}
    public List<Waypoint> visible(String dimension) {return entries.stream().filter(w->w.visible() && w.dimension().equals(dimension)).toList();}
    public boolean put(Waypoint waypoint) {
        var next=new ArrayList<>(entries);int index=-1;
        for(int i=0;i<next.size();i++)if(next.get(i).id().equals(waypoint.id())){index=i;break;}
        if(index<0)next.add(waypoint);else next.set(index,waypoint);
        return write(next);
    }
    public boolean delete(String id) {var next=new ArrayList<>(entries);next.removeIf(w->w.id().equals(id));return write(next);}
    private boolean write(List<Waypoint> next) {
        // Keep an unreadable file intact so a failed read cannot erase saved markers.
        if(!readable)return false;
        try {
            Files.createDirectories(directory);
            var data=new Data();data.waypoints=next;
            atomicWrite(directory.resolve("waypoints.json"),JSON.toJson(data));
            entries=next;return true;
        }catch(IOException e){HudStudio.LOGGER.error("Cannot save waypoints",e);return false;}
    }
    public static void atomicWrite(Path file,String value) throws IOException {
        Path temp=Files.createTempFile(file.getParent(),file.getFileName().toString(),".tmp");
        try {Files.writeString(temp,value);replace(temp,file);}finally{Files.deleteIfExists(temp);}
    }
    static void replace(Path temp,Path file) throws IOException {
        try {Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
    }
}
