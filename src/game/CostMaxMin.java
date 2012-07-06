package game;

public class CostMaxMin extends GenericGame {

	public CostMaxMin(int iClass, int iSite) {
		super(iClass, iSite);
	}

	@Override
	public void calculateWeight() {
		double[] daPredictionByClass = new double[iClass];
		double tmp = 0;
		/* calculate prediction by Class */
		for (int i = 0; i < iClass; i++) {
			tmp = 0;
			for (int j = 0; j < iSite; j++) {
				tmp += dmPricePerTask[i][j];
			}
			daPredictionByClass[i] = tmp;
		}

		/* calculate weight */
		for (int i = 0; i < iClass; i++) {
			// System.out.print("Weight[" + i + "]");
			for (int j = 0; j < iSite; j++) {
				/* the weight is 1(maximum), when the site is free */
				if (dmPricePerTask[i][j] == 0) {
					dmWeight[i][j] = 1;
				} else {
					dmWeight[i][j] = dmPricePerTask[i][j]
							/ daPredictionByClass[i];
				}
				// System.out.print(dmWeight[i][j] + ", ");
			}
			// System.out.println();
		}
	}

	@Override
	public void calculateInitDist() {
		/* calculate processing rate of each site */
		double tmp = 0, rest = 0;
		double[] daProcRateByClass = new double[iClass];
		for (int i = 0; i < iClass; i++) {
			tmp = 0;
			// System.out.print("ProcessRate[" + i + "]");
			for (int j = 0; j < iSite; j++) {
				dmProcessRate[i][j] = iaCPU[j] / dmPrediction[i][j];
				tmp += dmProcessRate[i][j];
				// System.out.print(dmProcessRate[i][j] + ", ");
			}
			// System.out.println();
			daProcRateByClass[i] = tmp;
		}
		/* distribute tasks according to the sort */
		int k;
		for (int i = 0; i < iClass; i++) {
			// System.out.print("0Distribution[" + i + "]");
			tmp = 0;
			rest = iaTask[i];
			for (int j = 0; j < iSite; j++) {
				if (rest != 0) {
					// the first site to distribute
					k = (int) dmRankResource[i][j];
					tmp = dDeadline * iaCPU[k] / dmPrediction[i][k];
					if (rest > tmp) {
						dmDist[i][k] = tmp;
						rest = rest - tmp;
					} else {
						dmDist[i][k] = rest;
						rest = 0;
					}
				} else {
					k = (int) dmRankResource[i][j];
					dmDist[i][k] = 0;
				}

			}
			// for (int j = 0; j < iSite; j++)
			// {
			// System.out.print(dmDistribution[i][j] + ", ");
			// }
			// System.out.println();
		}
	}

	boolean bDeadline = true;

	public boolean compDistribution() {
		bDeadline = true;
		boolean bUsedNewResource = false;
		double tmp = 0, rest = 0;
		/* distribute tasks according to the sort */
		int k;
		double lastAllocation;
		for (int i = 0; i < iClass; i++) {
			// System.out.print(iStage + "Distribution[" + i + "]");
			tmp = 0;
			rest = iaTask[i];
			for (int j = 0; j < iSite; j++) {
				if (rest != 0) {
					// the first site to distribute
					k = (int) dmRankResource[i][j];
					if (dmAlloc[i][k] == -1) {
						lastAllocation = iaCPU[j];
						bUsedNewResource = true;
					} else {
						lastAllocation = dmAlloc[i][k];
					}
					tmp = dDeadline * lastAllocation / dmPrediction[i][k];
					if (rest > tmp) {
						dmDist[i][k] = tmp;
						rest = rest - tmp;
					} else {
						dmDist[i][k] = rest;
						rest = 0;
						continue;
					}
				} else {
					k = (int) dmRankResource[i][j];
					dmDist[i][k] = 0;
				}
			}
			/* after distribution, the deadline can not be fulfilled */
			if (rest > 0) {
				bDeadline = false;
				return true;
			}
			// for (int j = 0; j < iSite; j++)
			// {
			// System.out.print(dmDistribution[i][j] + ", ");
			// }
			// System.out.println();
		}
		return bUsedNewResource;
	}

	public void compAllocation() {
		/* calculate processing rate of each site */
		double tmp = 0;
		double[] daRelativeWeightBySite = new double[iSite];
		for (int i = 0; i < iSite; i++) {
			tmp = 0;
			for (int j = 0; j < iClass; j++) {
				tmp += dmPrediction[j][i] * dmWeight[j][i]
						* dmDist[j][i];
			}
			// System.out.println("RelativeValue["+i+"] = "+ tmp);
			daRelativeWeightBySite[i] = tmp;
		}

		for (int i = 0; i < iSite; i++) {
			for (int j = 0; j < iClass; j++) {
				if (daRelativeWeightBySite[i] != 0) {
					dmAlloc[j][i] = (dmDist[j][i]
							* dmPrediction[j][i] * dmWeight[j][i] * iaCPU[i])
							/ daRelativeWeightBySite[i];
				} else {
					dmAlloc[j][i] = -1;
				}
			}
		}

		// for (int i = 0; i < iClass; i++)
		// {
		// System.out.print(iStage + "Allocation[" + i + "]");
		// for (int j = 0; j < iSite; j++)
		// {
		// System.out.print(dmAllocation[i][j] + ", ");
		// }
		// System.out.println();
		// }
	}

	public void compFinalResult() {
		do {
			iStage++;
			compAllocation();

			if (compDistribution()) {
				/* deadline can not be satisfied */
				if (!bDeadline) {
					System.out.println("THE DEADLINE CAN NOT BE SATISFIED!");
					return;
				} else {
					System.out.println("\nNEW ROUND WITHOUT CHECKING:");
					dEval = 1;
				}

			} else {
				compExecTime();
			}

			// System.out.println("Evaluation Value =========="+dEval);
		} while (dEval > 0);
		// while (evaluateResults());

		// System.out.println("==================Distribution=====================");
		for (int i = 0; i < iClass; i++) {
			// System.out.print("FinalDistribution[" + i + "] ");
			for (int j = 0; j < iSite; j++) {
				dmDist[i][j] = Math.round(dmDist[i][j]);
				// System.out.print(dmDistribution[i][j] + ",");
			}
			// System.out.println();
		}
		// System.out.println("==================Allocation=====================");
		for (int i = 0; i < iClass; i++) {
			System.out.print("FinalAllocation[" + i + "] ");
			for (int j = 0; j < iSite; j++) {
				dmAlloc[i][j] = Math.round(dmAlloc[i][j]);
				// System.out.print(dmAllocation[i][j] + ",");
			}
			// System.out.println();
		}
		// System.out.println("Stage = " + iStage);
	}

	public void compExecTime() {
		double newExeTime;
		double newCost;
		dEval = 0;
		dTime = 0;
		dCost = 0;
		for (int i = 0; i < iClass; i++) {
			newExeTime = 0;
			// System.out.print("Cost[" + i + "]");
			for (int j = 0; j < iSite; j++) {
				if (dmAlloc[i][j] != -1) {
					if (dmAlloc[i][j] < 1) {
						newExeTime = 0;
					} else {
						newExeTime = (dmDist[i][j] * dmPrediction[i][j])
								/ dmAlloc[i][j];
						if (newExeTime > dDeadline + 1) {
							newExeTime = Double.MAX_VALUE;
						}
					}
				}
				if (newExeTime > dDeadline + 1) {
					// System.out.println("newExeTime - dDeadline="+ (newExeTime
					// - dDeadline -1));
					newCost = Double.MAX_VALUE;
				} else {
					newCost = dmDist[i][j] * dmPrediction[i][j]
							* daPrice[j];
				}

				dTime += newExeTime;
				dCost += newCost;

				dEval += dmCost[i][j] - dCost;
				dmExeTime[i][j] = newExeTime;
				dmCost[i][j] = newCost;
				// System.out.print(dmCost[i][j] + ", ");
			}
			// System.out.println();
		}
		for (int i = 0; i < iClass; i++) {
			System.out.print("Time[" + i + "]");
			for (int j = 0; j < iSite; j++) {
				System.out.print(dmExeTime[i][j] + ", ");
			}
			System.out.println();
		}

		// System.out.println("AllTime = " + dTime + " AllCost = " + dCost);
		// System.out.println();
	}

	/**
	 * Sort reosurces for each class and get one sorted matrix back
	 */
	public void sortResources() {
		/* compute cost per acitivity */
		for (int i = 0; i < iClass; i++) {
			// System.out.print("PricePerActivity[" + i + "] ");
			for (int j = 0; j < iSite; j++) {
				dmPricePerTask[i][j] = daPrice[j] * dmPrediction[i][j];
				// System.out.print(j + ":" + dmPricePerActivity[i][j] + ", ");
			}
			// System.out.println();
		}
		double[][] array = new double[iSite][2];
		/* sort every class */
		for (int i = 0; i < iClass; i++) {
			// init array
			for (int j = 0; j < iSite; j++) {
				array[j][0] = dmPricePerTask[i][j];
				array[j][1] = j;
			}
			QuickSort.sort(array, 0, iSite - 1);
			// System.out.print("RANK[" + i + "] ");
			for (int j = 0; j < iSite; j++) {
				dmRankResource[i][j] = array[j][1];
				// System.out.print(dmRankResource[i][j] + ", ");
			}
			// System.out.println();
		}

	}

	/**
	 * Sort reosurces for each class and get one sorted matrix back
	 */
	public void sortClass() {
		/* compute cost per acitivity */
		for (int i = 0; i < iClass; i++) {
			// System.out.print("PricePerActivity[" + i + "] ");
			for (int j = 0; j < iSite; j++) {
				dmPricePerTask[i][j] = daPrice[j] * dmPrediction[i][j];
				// System.out.print(j + ":" + dmPricePerActivity[i][j] + ", ");
			}
			// System.out.println();
		}
		double[][] array = new double[iClass][2];
		/* sort every class */
		for (int i = 0; i < iSite; i++) {
			// init array
			for (int j = 0; j < iClass; j++) {
				array[j][0] = dmPrediction[j][i];
				array[j][1] = j;
			}
			QuickSort.sort(array, 0, iClass - 1);
			// System.out.print("RANK CLASS[" + i + "] ");
			for (int j = 0; j < iClass; j++) {
				dmRankClass[i][j] = array[j][1];
				// System.out.print(dmRankClass[i][j] + ", ");
			}
			// System.out.println();
		}

	}

	public double maxmin() {
		sortResources();
		sortClass();
		calculateWeight();

		dmMinMakespan = new double[iClass][iSite][iCPUMaxNum];
		dmMinminCost = new double[iSite][iCPUMaxNum];
		dmMinminTime = new double[iSite][iCPUMaxNum];

		double[] daMinminTimeBySite = new double[iSite];
		// find the current cheapest site for the acitvities.

		initMinMakespan();
		while (getRestLength() > 0) {
			chooseMaxMinMakespan();
			if (iMinClass < 0 | iMinSite < 0 | iMinCPU < 0) {
				System.out.println("CANNOT MEET DEADLINE!!");
				return 0;
			}
			updateMin();
			iStage++;
		}
		double sumTime = 0, sumCost = 0;
		double tmpTime = -1;
		for (int i = 0; i < iSite; i++) {
			// System.out.print("Time(Site*CPU)["+i+"]");
			for (int j = 0; j < iaCPU[i]; j++) {
				// System.out.print(dmMinminTime[i][j] + ", ");
				daMinminTimeBySite[i] += dmMinminTime[i][j];
				if (tmpTime < dmMinminTime[i][j]) {
					tmpTime = dmMinminTime[i][j];
				}

				sumTime += dmMinminTime[i][j];
				sumCost += dmMinminCost[i][j];
			}
			// System.out.println();
		}
		// for (int i = 0; i < iClass; i++)
		// {
		// System.out.print(iStage + "Distribution[" + i + "]");
		// for (int j = 0; j < iSite; j++)
		// {
		// System.out.print(dmDistribution[i][j] + ", ");
		// }
		// System.out.println();
		// }
		System.out.println("MaxMin Time= " + sumTime);
		System.out.println("MakeSpan   = " + tmpTime);
		dFinalMakespan = tmpTime;
		dCost = sumCost;
		dTime = sumTime;
		if (dFinalMakespan > dDeadline) {
			System.out.println("CANNOT MEET DEADLINE!!!");
		}
		return sumTime;

	}

	int getRestLength() {
		int sum = 0;
		// init array
		for (int j = 0; j < iClass; j++) {
			sum += iaQueuedTask[j];
		}
		return sum;
	}

	void initMinMakespan() {
		for (int i = 0; i < iClass; i++) {
			for (int j = 0; j < iSite; j++) {
				for (int k = 0; k < iaCPU[j]; k++) {
					dmMinMakespan[i][j][k] = dmPrediction[i][j];
				}
			}
		}
	}

	void chooseMaxMinMakespan() {
		/* Find the min makespan in one class, and then find the max one */
		double tmpMinMakespan = Double.MAX_VALUE;
		double tmpMaxMinMakespan = -1;
		double tmpMinCost = Double.MAX_VALUE;
		double tmpMaxMinCost = -1;

		int iTmpMinClass = 0;
		int iTmpMinSite = 0;
		int iTmpMinCPU = 0;

		iMinClass = -1;
		iMinSite = -1;
		iMinCPU = -1;

		for (int i = 0; i < iClass; i++) {
			for (int j = 0; j < iSite; j++) {
				for (int k = 0; k < iaCPU[j]; k++) {
					if (tmpMinMakespan > dmMinMakespan[i][j][k]
							& tmpMinCost > daPrice[j] * dmPrediction[i][j]
							& dDeadline > dmMinMakespan[i][j][k]) {
						iTmpMinClass = i;
						iTmpMinSite = j;
						iTmpMinCPU = k;
						tmpMinMakespan = dmMinMakespan[i][j][k];
						tmpMinCost = daPrice[j] * dmPrediction[i][j];
					}
				}
			}
			if (tmpMaxMinMakespan < tmpMinMakespan) {
				iMinClass = i;
				iMinSite = iTmpMinSite;
				iMinCPU = iTmpMinCPU;
				tmpMaxMinMakespan = tmpMinMakespan;
				tmpMaxMinCost = tmpMinCost;
			}
			tmpMinMakespan = Double.MAX_VALUE;
			tmpMinCost = Double.MAX_VALUE;
			iTmpMinClass = -1;
			iTmpMinSite = -1;
			iTmpMinCPU = -1;

		}
	}

	void updateMin() {
		dmMinminTime[iMinSite][iMinCPU] += dmPrediction[iMinClass][iMinSite];
		dmMinminCost[iMinSite][iMinCPU] += dmPricePerTask[iMinClass][iMinSite];
		for (int i = 0; i < iClass; i++) {
			dmMinMakespan[i][iMinSite][iMinCPU] += dmPrediction[iMinClass][iMinSite];
		}

		iaQueuedTask[iMinClass]--;
		if (iaQueuedTask[iMinClass] == 0) {
			for (int j = 0; j < iSite; j++) {
				for (int k = 0; k < iaCPU[j]; k++) {
					dmMinMakespan[iMinClass][j][k] = -1;
				}
			}
		}

		dmDist[iMinClass][iMinSite]++;
	}

	public void test1() {
		this.iClass = 2;
		this.iSite = 2;
		dmPrediction = new double[iClass][iSite];
		iaTask = new int[iClass];
		iaQueuedTask = new int[iClass];
		dmWeight = new double[iClass][iSite];
		dmAlloc = new double[iClass][iSite];
		dmDist = new double[iClass][iSite];
		dmRankResource = new double[iClass][iSite];
		dmRankClass = new double[iSite][iClass];
		dmPricePerTask = new double[iClass][iSite];
		daPrice = new double[iSite];
		iaCPU = new int[iSite];
		dmProcessRate = new double[iClass][iSite];
		dmExeTime = new double[iClass][iSite];
		dmCost = new double[iClass][iSite];
		this.iClass = 2;
		this.iSite = 2;
		iaTask[0] = 2;
		iaTask[1] = 2;
		iaQueuedTask[0] = 2;
		iaQueuedTask[1] = 2;

		iCPUMaxNum = 2;

		iaCPU[0] = 2;
		iaCPU[1] = 2;

		dmPrediction[0][0] = 1;
		dmPrediction[0][1] = 1;
		dmPrediction[1][0] = 1;
		dmPrediction[1][1] = 1;

		for (int j = 0; j < iSite; j++) {
			iAllCPU += iaCPU[j];
		}
		maxmin();
	}

	public void test2() {
		this.iClass = 3;
		this.iSite = 3;

		dmPrediction = new double[iClass][iSite];
		iaTask = new int[iClass];
		iaQueuedTask = new int[iClass];
		dmWeight = new double[iClass][iSite];
		dmAlloc = new double[iClass][iSite];
		dmDist = new double[iClass][iSite];
		dmRankResource = new double[iClass][iSite];
		dmRankClass = new double[iSite][iClass];
		dmPricePerTask = new double[iClass][iSite];
		daPrice = new double[iSite];
		iaCPU = new int[iSite];
		dmProcessRate = new double[iClass][iSite];
		dmExeTime = new double[iClass][iSite];
		dmCost = new double[iClass][iSite];

		iaTask[0] = 10000;
		iaTask[1] = 10000;
		iaTask[2] = 10000;
		iaQueuedTask[0] = 10000;
		iaQueuedTask[1] = 10000;
		iaQueuedTask[2] = 10000;

		iaCPU[0] = 10;
		iaCPU[1] = 10;
		iaCPU[2] = 10;

		daPrice[0] = 1.5;
		daPrice[1] = 1.2;
		daPrice[2] = 1;

		dmPrediction[0][0] = 1;
		dmPrediction[0][1] = 2;
		dmPrediction[0][2] = 3;
		dmPrediction[1][0] = 10;
		dmPrediction[1][1] = 12;
		dmPrediction[1][2] = 15;
		dmPrediction[2][0] = 23;
		dmPrediction[2][1] = 25;
		dmPrediction[2][2] = 21;

		for (int j = 0; j < iSite; j++) {
			iAllCPU += iaCPU[j];
		}

		// System.out.println("=============MINMIN===================");
		maxmin();
	}

	public void test3() {
		this.iClass = 100;
		this.iSite = 100;

		dmPrediction = new double[iClass][iSite];
		iaTask = new int[iClass];
		dmWeight = new double[iClass][iSite];
		dmAlloc = new double[iClass][iSite];
		dmDist = new double[iClass][iSite];
		daPrice = new double[iSite];
		iaCPU = new int[iSite];
		dmProcessRate = new double[iClass][iSite];
		dmExeTime = new double[iClass][iSite];

		for (int j = 0; j < iClass; j++) {
			iaTask[j] = Math.round(Math.round(100000 * Math.random()));
		}
		for (int j = 0; j < iSite; j++) {
			iaCPU[j] = 64;
		}

		for (int i = 0; i < iClass; i++) {
			for (int j = 0; j < iSite; j++) {
				dmPrediction[i][j] = 1 + Math.random() * 10;
			}
		}

		for (int j = 0; j < iSite; j++) {
			daPrice = new double[j];
			iAllCPU += iaCPU[j];
		}

		// System.out.println("=============MINMIN===================");
		maxmin();
	}

	public static void main(String[] args) {
		MaxMin maxmint = new MaxMin(2, 2);
		maxmint.test1();
	}

}
