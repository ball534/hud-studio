package dev.hudstudio.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Original pixel glyphs for the Studio module cards. */
public final class StudioIcons {
    public static void draw(GuiGraphicsExtractor g,String id,int x,int y) {
        String glyph=switch(id) {
            case "scoreboard" -> "wwwwwwwww/wgggggggw/wl.lll..w/wl..ll..w/wl...l..w/wl..ll..w/wwwwwwwww";
            case "bossbar" -> "..ppppp../.pplplpp./ppppppppp/.p.p.p.p./........./kkkkkkkkk/krrrrrssk/kkkkkkkkk";
            case "player_tab" -> "kkkkkkkkk/kvvvvvvvk/kl..lll.k/kl..lll.k/k.......k/kl..lll.k/kl..lll.k/kkkkkkkkk";
            case "effects" -> "...www.../...ggg.../....l..../...l.l.../..l...l../.l.ppp.l./.lplpppl./..lllll..";
            case "offhand" -> "wwwwwwwww/wsslllssw/wsslllssw/.wslllsw./.wslllsw./..wlllw../...www...";
            case "wasd" -> "....lll..../....lsl..../....kkk..../.........../lll.lll.lll/lsl.lsl.lsl/kkk.kkk.kkk";
            case "mouse" -> mouse("bbb","bbb","");
            case "left_cps" -> mouse("ggg","sss","/.g.g....");
            case "right_cps" -> mouse("sss","ggg","/....g.g.");
            case "space" -> "llllllllllll/lssssssssssl/ls..llll..sl/lssssssssssl/kkkkkkkkkkkk";
            case "sneak" -> ".....vv../.....vv../....bbb../...bbbvv./...bb..../..pppp.../....pp.../.wwwwwww.";
            case "sprint" -> ".......vv/.......vv/....bbb../.s.bbbvv./...bb..../s.ppppp../.pp...pp./kk.....pp";
            case "fps" -> "kkkkkkkkk/klssssslk/klbsssslk/klbbssslk/klbsssslk/lllllllll/...kkk...";
            case "speed" -> "..lllll../.lssssl../lgl...bl./l....bl../l...bl..l/l..ll...l/.lsssssl./..lllll..";
            case "clock" -> "...ggg.../..glllg../.glllllg./glllslllg/glllssllg/glllllllg/.glllllg./..glllg../...ggg...";
            case "memory" -> "kkkkkkkkkkk/kvvvvvvvvvk/kvkkvkkvkvk/kvkkvkkvkvk/kvvvvvvvvvk/.gggg.gggg.";
            case "add" -> "lllllllll/lsssgsssk/lsssgsssk/lsgggggsk/lsssgsssk/lsssgsssk/lkkkkkkkk";
            case "combo" -> ".......ll/......lbk/.....lbk./.g..lbk../..glbk.../...gk..g./..wg..ggg/.wk....g.";
            case "zoom" -> "..gggg.../.gbbbbg../gbbllbbg./gbblbbbg./gbbbbbbg./.gbbbbgw./..gggg.ww/........w";
            case "no_shake" -> "s..lll..s/.slkkkls./slkbblkl./..kbbb.k./slkbbbkl./.slkkkls./s..lll..s";
            case "full_bright" -> "....g..../.g..l..g./..gglgg../..glllg../glllllllg/..glllg../..gglgg../.g..l..g./....g....";
            case "freecam" -> "...b.b.../...bbb.../.kkkkkkk./kssbbsskr/ksblbbskr/ksbbbbssk/kssbbsskr/.kkkkkkk.";
            case "freelook" -> ".....bbb../........b./..lllll.../.lbkkkbl.b/lbklllkkbl/.lbkkkbl../..lllll.../.bbb......";
            case "no_explosions" -> "...wwg.../.....g.../.rrrrrrr./.rlllllr./.rlllllr./.rrrrrrr./........l/..l...ll./...lll...";
            default -> "lllllllll/lsslsslsk/lsklsslsk/lllllllll/lsslsslsk/lsslsslsk/lkkkkkkkk";
        };
        String[] rows=glyph.split("/");
        int width=0;
        for(String row:rows)width=Math.max(width,row.length());
        int left=x+(16-width)/2,top=y+(16-rows.length)/2;
        for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++) {
            int color=switch(rows[row].charAt(col)) {
                case 'k' -> 0xff161613;
                case 's' -> 0xff99968d;
                case 'l' -> 0xffe7e3d7;
                case 'g' -> 0xffefbe55;
                case 'r' -> 0xffec6558;
                case 'v' -> 0xff83b956;
                case 'b' -> 0xff79bad6;
                case 'p' -> 0xffbf8bd4;
                case 'w' -> 0xffac7850;
                default -> 0;
            };
            if(color!=0)g.fill(left+col,top+row,left+col+1,top+row+1,color);
        }
    }

    private static String mouse(String left,String right,String indicator) {
        return ".kkkkkkk./k"+left+"k"+right+"k/k"+left+"k"+right+"k/kkkkkkkkk/ksssssssk/ksssssssk/.kkkkkkk."+indicator;
    }
}
