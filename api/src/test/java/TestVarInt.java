import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mdproxy.VarInt;

public class TestVarInt {

    @Test
    public void testVarInt() {

        int[] values = new int[] { 0,1, 56, 127, 128, 255, 256, -1, -555, 65535, 65536, 16777215, 16777216, Integer.MAX_VALUE, Integer.MIN_VALUE };

        ByteBuf buffer = Unpooled.buffer();
        for(int val : values) {

            VarInt varInt = new VarInt(val);
            varInt.write(buffer);

            try {
                VarInt varInt2 = VarInt.read(buffer);
                Assertions.assertEquals(varInt.value(), varInt2.value());
            } catch (Exception ex) {
                Assertions.fail("Encountered an exeception while reading " + val + "!", ex);
            }

        }

    }

}
