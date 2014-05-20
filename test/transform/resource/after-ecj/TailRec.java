import java.math.BigInteger;

public class TailRec {
	
	private static final BigInteger ONE = BigInteger.ONE;
	
	public BigInteger fac(BigInteger n, BigInteger ret) {
		return fac$internal__(n, ret).runRecursiveFunction();
	}
	
	@java.lang.SuppressWarnings("all")
	private final de.kdi.tailcalls.TailCallExpression<BigInteger> fac$internal__(final BigInteger n, final BigInteger ret) {
		if (n.equals(ONE)) {
			return new de.kdi.tailcalls.TailCallExpression.Done<BigInteger>(ret);
		} else {
			return new de.kdi.tailcalls.TailCallExpression.TailCall<BigInteger>(){
				public de.kdi.tailcalls.TailCallExpression<BigInteger> apply() {
					return fac$internal__(n.subtract(ONE), ret.multiply(n));
				}
				
			};
		}
	}
	
}
