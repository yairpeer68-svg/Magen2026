package com.magen.family.server;

import com.magen.family.visual.ShortFormFingerprint;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class ShortFormMatchLogicTest {
    private ShortFormFingerprint fp(String frame,String center,String text,String... evidence){
        return new ShortFormFingerprint(frame,center,text,Arrays.asList(evidence),Arrays.asList());
    }

    @Test public void exactTextHashMatches(){
        ShortFormFingerprint f=fp("0123456789abcdef","fedcba9876543210","aaaaaaaaaaaaaaaa", "1111111111111111");
        assertEquals("text_hash",ShortFormMatchLogic.reason(f,"0","0","aaaaaaaaaaaaaaaa",new HashSet<>(),new HashSet<>()));
    }

    @Test public void oneWeakEvidenceIsNotEnoughButTwoAre(){
        ShortFormFingerprint f=fp("0123456789abcdef","fedcba9876543210","", "1111111111111111","2222222222222222");
        Set<String> one=new HashSet<>(Arrays.asList("1111111111111111"));
        assertNull(ShortFormMatchLogic.reason(f,"0","0","",one,new HashSet<>()));
        one.add("2222222222222222");
        assertEquals("evidence_2plus",ShortFormMatchLogic.reason(f,"0","0","",one,new HashSet<>()));
    }

    @Test public void oneStrongEvidenceStillCannotMatchAlone(){
        ShortFormFingerprint f=new ShortFormFingerprint("","","",Arrays.asList("aaaaaaaabbbbbbbb"),Arrays.asList("aaaaaaaabbbbbbbb"));
        assertNull(ShortFormMatchLogic.reason(f,"","","",new HashSet<>(Arrays.asList("aaaaaaaabbbbbbbb")),new HashSet<>(Arrays.asList("aaaaaaaabbbbbbbb"))));
    }

    @Test public void visualRequiresBothHashesClose(){
        assertTrue(ShortFormMatchLogic.visualPair("0000000000000000","0000000000000000","0000000000000003","0000000000000005"));
        assertFalse(ShortFormMatchLogic.visualPair("0000000000000000","0000000000000000","000000000000001f","0000000000000001"));
        assertFalse(ShortFormMatchLogic.visualPair("0000000000000000","0000000000000000","0000000000000001","000000000000001f"));
    }
}
