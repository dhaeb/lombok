import java.math.BigInteger;

import lombok.experimental.TailCall;

public class TailRecIfs {
	
	private static final BigInteger ONE = BigInteger.ONE;
	
	@TailCall 
	public BigInteger fac(BigInteger n, BigInteger ret) {
		if (n.equals(ONE)) 
			return ret;
		else 
			return fac(n.subtract(ONE), ret.multiply(n));
	}
	
}
