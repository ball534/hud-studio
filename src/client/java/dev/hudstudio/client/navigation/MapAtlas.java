package dev.hudstudio.client.navigation;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hudstudio.HudStudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Samples only client-loaded chunks. Disk workers never touch Minecraft world data. */
public final class MapAtlas implements AutoCloseable {
    public static final int SIZE=128;
    private static final int MAGIC=0x48534d32,LEGACY_MAGIC=0x48534d31,MAX_REGIONS=256;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"HUD Studio map storage");t.setDaemon(true);return t;});
    private record Snapshot(int[] colors,short[] heights,long chunks) {}
    private static final ConcurrentMap<Path,Snapshot> PENDING=new ConcurrentHashMap<>();
    private record Key(String dimension,int x,int z) {}
    private record Chunk(int x,int z) {}
    private final Path directory;
    private final String texturePrefix;
    private final Map<String,Set<Long>> known=new LinkedHashMap<>();
    private final LinkedHashMap<Key,Region> regions=new LinkedHashMap<>(32,.75f,true);
    private final Map<Long,Long> sampled=new HashMap<>();
    private final ArrayDeque<Chunk> queue=new ArrayDeque<>();
    private String samplingDimension="";
    private long ticks;
    private boolean closed;
    public static final class Region {
        public final int[] colors=new int[SIZE*SIZE];
        public final short[] heights=new short[SIZE*SIZE];
        private boolean dirty;
        private long chunks;
        private long revision,uploaded=-1;
        private DynamicTexture texture;
        private Identifier textureId;
    }
    public MapAtlas(Path worldDirectory) {
        directory=worldDirectory.resolve("maps");texturePrefix=worldDirectory.getFileName().toString();
        if(Files.isDirectory(directory))try(var dirs=Files.list(directory)) {
            dirs.filter(Files::isDirectory).forEach(dir->{
                try {
                    String dimension=new String(Base64.getUrlDecoder().decode(dir.getFileName().toString()),StandardCharsets.UTF_8);
                    // Only actual dimension IDs belong in the map index.
                    if(Identifier.tryParse(dimension)!=null)index(dimension);
                }
                catch(IllegalArgumentException ignored){}
            });
        }catch(IOException e){HudStudio.LOGGER.warn("Cannot index saved maps",e);}
    }
    private Path dimensionDirectory(String dimension) {return directory.resolve(Base64.getUrlEncoder().withoutPadding().encodeToString(dimension.getBytes(StandardCharsets.UTF_8)));}
    private Path file(Key key) {return dimensionDirectory(key.dimension).resolve(key.x+"_"+key.z+".hmap");}
    private static long packed(int x,int z) {return (long)x<<32 | z&0xffffffffL;}
    private Set<Long> index(String dimension) {
        var existing=known.get(dimension);if(existing!=null)return existing;
        Set<Long> result=new HashSet<>();known.put(dimension,result);
        Path dir=dimensionDirectory(dimension);
        if(Files.isDirectory(dir))try(var paths=Files.list(dir)) {
            paths.forEach(path->{String[] pair=path.getFileName().toString().replace(".hmap","").split("_");
                if(!path.toString().endsWith(".hmap") || pair.length!=2)return;
                try{result.add(packed(Integer.parseInt(pair[0]),Integer.parseInt(pair[1])));}catch(NumberFormatException ignored){}
            });
        }catch(IOException e){HudStudio.LOGGER.warn("Cannot index map dimension {}",dimension,e);}
        return result;
    }
    public List<String> dimensions() {
        var result=new LinkedHashSet<>(List.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end"));
        result.addAll(known.keySet());return List.copyOf(result);
    }
    public Region region(String dimension,int rx,int rz,boolean create) {
        if(closed)return null;
        Key key=new Key(dimension,rx,rz);Region region=regions.get(key);if(region!=null)return region;
        var available=index(dimension);
        if(!create && !available.contains(packed(rx,rz)) && !PENDING.containsKey(file(key)))return null;
        region=new Region();Path path=file(key);var pending=PENDING.get(path);
        if(pending!=null) {System.arraycopy(pending.colors,0,region.colors,0,region.colors.length);System.arraycopy(pending.heights,0,region.heights,0,region.heights.length);region.chunks=pending.chunks;}
        else if(available.contains(packed(rx,rz)))try(var in=new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            int magic=in.readInt();if(magic!=MAGIC && magic!=LEGACY_MAGIC)throw new IOException("Unsupported map format");
            if(magic==MAGIC)region.chunks=in.readLong();
            for(int i=0;i<region.colors.length;i++){region.colors[i]=in.readInt();region.heights[i]=in.readShort();}
        }catch(IOException e){HudStudio.LOGGER.warn("Cannot read map tile {}",path,e);available.remove(packed(rx,rz));Arrays.fill(region.colors,0);}
        if(create)available.add(packed(rx,rz));
        regions.put(key,region);
        if(regions.size()>MAX_REGIONS) {
            var oldest=regions.entrySet().iterator();var entry=oldest.next();save(entry.getKey(),entry.getValue());release(entry.getValue());oldest.remove();
        }
        return region;
    }
    public Identifier texture(String dimension,int rx,int rz,Region region) {
        if(region.texture==null) {
            region.textureId=Identifier.fromNamespaceAndPath("hud-studio","map/"+texturePrefix+"/"+Navigation.hash(dimension)+"/"+rx+"_"+rz);
            region.texture=new DynamicTexture(()->"HUD Studio explored map",new NativeImage(SIZE,SIZE,true));
            Minecraft.getInstance().getTextureManager().register(region.textureId,region.texture);
        }
        if(region.uploaded!=region.revision) {
            var image=region.texture.getPixels();
            for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++)image.setPixel(x,z,region.colors[z*SIZE+x]);
            region.texture.upload();region.uploaded=region.revision;
        }
        return region.textureId;
    }
    public Integer height(String dimension,int x,int z) {
        var region=region(dimension,Math.floorDiv(x,SIZE),Math.floorDiv(z,SIZE),false);if(region==null)return null;
        int i=Math.floorMod(z,SIZE)*SIZE+Math.floorMod(x,SIZE);return region.colors[i]==0?null:(int)region.heights[i];
    }
    private static long chunkBit(int cx,int cz) {return 1L<<(Math.floorMod(cz,8)*8+Math.floorMod(cx,8));}
    private boolean recorded(String layer,int cx,int cz) {
        var r=region(layer,Math.floorDiv(cx,8),Math.floorDiv(cz,8),false);
        return r!=null && (r.chunks&chunkBit(cx,cz))!=0;
    }
    public void chunkLoaded() {queue.clear();}
    public void unloading(net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.level.chunk.LevelChunk chunk) {
        var mc=Minecraft.getInstance();if(mc.level!=level || closed)return;
        int cx=chunk.getPos().x(),cz=chunk.getPos().z();
        if(!recorded(Navigation.dimension(),cx,cz))sample(mc,Navigation.dimension(),chunk);
    }
    public void tick(Minecraft mc) {
        if(closed || mc.level==null || mc.player==null)return;
        ticks++;String dimension=Navigation.dimension();
        if(!dimension.equals(samplingDimension)){queue.clear();sampled.clear();samplingDimension=dimension;ticks=1;}
        // Rebuild frequently, always taking unsurveyed chunks before refresh work.
        // A failed/stale chunk lookup never counts as successfully surveyed.
        if(ticks%10==1 || queue.isEmpty()) {
            int cx=mc.player.chunkPosition().x(),cz=mc.player.chunkPosition().z();
            int radius=Math.min(32,mc.options.getEffectiveRenderDistance());var chunks=new ArrayList<Chunk>();
            for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++) {
                int x=cx+dx,z=cz+dz;
                if(mc.level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false)!=null
                    && (!recorded(dimension,x,z) || ticks-sampled.getOrDefault(packed(x,z),-1000L)>=200))chunks.add(new Chunk(x,z));
            }
            chunks.sort(Comparator.<Chunk>comparingInt(c->recorded(dimension,c.x,c.z)?1:0)
                .thenComparingInt(c->(c.x-cx)*(c.x-cx)+(c.z-cz)*(c.z-cz)));
            queue.clear();queue.addAll(chunks);
            sampled.keySet().removeIf(k->Math.abs((int)(k>>32)-cx)>radius+2 || Math.abs((int)(long)k-cz)>radius+2);
        }
        long deadline=System.nanoTime()+3_000_000;
        for(int i=0;i<4 && !queue.isEmpty();i++) {
            var p=queue.removeFirst();var chunk=mc.level.getChunkSource().getChunk(p.x,p.z,ChunkStatus.FULL,false);
            if(chunk!=null) {sample(mc,dimension,chunk);sampled.put(packed(p.x,p.z),ticks);}
            if(System.nanoTime()>deadline)break;
        }
        if(ticks%200==0)flush();
    }
    private record Column(net.minecraft.world.level.block.state.BlockState state,int y,int depth) {}
    private static Column column(net.minecraft.world.level.chunk.LevelChunk chunk,int wx,int wz,BlockPos.MutableBlockPos pos) {
        int minimum=chunk.getMinY(),y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,Math.floorMod(wx,16),Math.floorMod(wz,16));
        var state=chunk.getBlockState(pos.set(wx,y,wz));
        while(y>minimum && (state.isAir() || state.getMapColor(chunk,pos)==MapColor.NONE))state=chunk.getBlockState(pos.set(wx,--y,wz));
        int depth=0;
        if(!state.getFluidState().isEmpty()) {
            int below=y;while(below>minimum && !chunk.getBlockState(pos.set(wx,below,wz)).getFluidState().isEmpty()){depth++;below--;}
        }
        pos.set(wx,y,wz);return new Column(state,y,depth);
    }
    /** Vanilla scale-zero filled-map palette and the exact vanilla shade thresholds. */
    public static int vanillaColor(MapColor color,int y,int north,int waterDepth,int x,int z) {
        MapColor.Brightness shade;
        if(color==MapColor.WATER) {
            double depth=waterDepth*.1+((x+z)&1)*.2;
            shade=depth<.5?MapColor.Brightness.HIGH:depth>.9?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
        }else {
            double slope=(y-north)*4.0/5.0+(((x+z)&1)-.5)*.4;
            shade=slope>.6?MapColor.Brightness.HIGH:slope<-.6?MapColor.Brightness.LOW:MapColor.Brightness.NORMAL;
        }
        return color==MapColor.NONE?0:color.calculateARGBColor(shade);
    }
    private void sample(Minecraft mc,String layer,net.minecraft.world.level.chunk.LevelChunk chunk) {
        int cx=chunk.getPos().x(),cz=chunk.getPos().z();
        var region=region(layer,Math.floorDiv(cx,8),Math.floorDiv(cz,8),true);var pos=new BlockPos.MutableBlockPos();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            int wx=cx*16+x,wz=cz*16+z;var col=column(chunk,wx,wz,pos);
            int north;
            if(z>0)north=region.heights[Math.floorMod(wz-1,SIZE)*SIZE+Math.floorMod(wx,SIZE)];
            else {
                var neighbour=mc.level.getChunkSource().getChunk(cx,cz-1,ChunkStatus.FULL,false);
                north=neighbour==null?col.y:column(neighbour,wx,wz-1,pos).y;
            }
            pos.set(wx,col.y,wz);int color=vanillaColor(col.state.getMapColor(mc.level,pos),col.y,north,col.depth,wx,wz);
            int index=Math.floorMod(wz,SIZE)*SIZE+Math.floorMod(wx,SIZE);
            if(region.colors[index]!=color || region.heights[index]!=col.y){region.colors[index]=color;region.heights[index]=(short)col.y;region.dirty=true;region.revision++;}
        }
        long mask=chunkBit(cx,cz);if((region.chunks&mask)==0){region.chunks|=mask;region.dirty=true;}
    }
    public void flush() {regions.forEach(this::save);}
    private void save(Key key,Region region) {
        if(!region.dirty)return;region.dirty=false;
        var snapshot=new Snapshot(region.colors.clone(),region.heights.clone(),region.chunks);Path path=file(key);PENDING.put(path,snapshot);
        IO.execute(()->{
            Path temp=null;
            try {
                Files.createDirectories(path.getParent());temp=Files.createTempFile(path.getParent(),"tile-",".tmp");
                try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temp)))) {
                    out.writeInt(MAGIC);out.writeLong(snapshot.chunks);for(int i=0;i<snapshot.colors.length;i++){out.writeInt(snapshot.colors[i]);out.writeShort(snapshot.heights[i]);}
                }
                WaypointStore.replace(temp,path);PENDING.remove(path,snapshot);
            }catch(IOException e){HudStudio.LOGGER.error("Cannot save explored map {}",path,e);}
            finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
        });
    }
    private void release(Region region) {if(region.textureId!=null)Minecraft.getInstance().getTextureManager().release(region.textureId);}
    @Override public void close() {flush();regions.values().forEach(this::release);regions.clear();closed=true;}
    static void deleteSavedMap(Path worldDirectory) {
        Path maps=worldDirectory.resolve("maps");
        // Run behind queued writes so an older snapshot cannot recreate a deleted map.
        IO.execute(()->{
            if(!Files.isDirectory(worldDirectory,LinkOption.NOFOLLOW_LINKS)
                || !Files.exists(maps,LinkOption.NOFOLLOW_LINKS))return;
            try(var paths=Files.walk(maps)) {
                var entries=paths.sorted(Comparator.reverseOrder()).toList();
                for(Path path:entries)Files.deleteIfExists(path);
                PENDING.keySet().removeIf(path->path.startsWith(maps));
            }catch(IOException | UncheckedIOException e){HudStudio.LOGGER.warn("Cannot remove map data for deleted world {}",worldDirectory,e);}
        });
    }
    public static void finishWrites() {
        try {IO.submit(()->{}).get(5,TimeUnit.SECONDS);}catch(Exception e){HudStudio.LOGGER.warn("Map storage did not finish before shutdown",e);}
    }
}
