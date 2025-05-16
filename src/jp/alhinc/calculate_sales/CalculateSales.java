package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";
	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";
	//new商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LIST = "commodity.lst";
	//new商品別ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String NOT_EXIST = "が存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String FILE_NOT_CONTINUOUS = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_DIGIT_OVER = "合計⾦額が10桁を超えました";
	private static final String BRANCHCODE_INVALID_FORMAT = "の支店コードが不正です";
	private static final String COMMODITYCODE_INVALID_FORMAT = "の商品コードが不正です";
	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		Map<String, String> branchNames = new HashMap<>();
		Map<String, Long> branchSales = new HashMap<>();
		Map<String, String> commodityNames = new HashMap<>();
		Map<String, Long> commoditySales = new HashMap<>();

		String branchRegex = ("^[0-9]{3}$");
		String commodityRegex = ("^[a-zA-Z0-9]{8}$");
		String branchList = "支店定義ファイル";
		String commodityList = "商品定義ファイル";

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchList, branchNames, branchSales, branchRegex)) {
			return;
		}
		//商品定義ファイルの読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LIST, commodityList, commodityNames, commoditySales, commodityRegex)) {
			return;
		}

		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();
		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile() && files[i].getName().matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}
		//ファイルが連番ではないときのエラー処理
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() -1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));
			if((latter - former) != 1) {
				System.out.println(FILE_NOT_CONTINUOUS);
				return;
			}
		}
		//rcdファイル数分繰り返す
		for(int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);
				String line;
				List <String> saleList = new ArrayList<String>();
				while((line = br.readLine()) != null) {
					saleList.add(line);
				}
				//ファイルの行数が不正のエラー処理
				if(saleList.size() != 3) {
					System.out.println(rcdFiles.get(i).getName() + FILE_INVALID_FORMAT);
					return;
				}
				//売り上げファイルの支店コードが支店定義ファイルにないときのエラー処理
				if(!branchNames.containsKey(saleList.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + BRANCHCODE_INVALID_FORMAT);
					return;
				}
				if(!commodityNames.containsKey(saleList.get(1))) {
					System.out.println(rcdFiles.get(i).getName() + COMMODITYCODE_INVALID_FORMAT);
					return;
				}
				//売り上げ金額が数字ではないときのエラー処理
				if(!saleList.get(2).matches("^[0-9]+$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}
				long fileSale = Long.parseLong(saleList.get(2));
				Long brunchAmount = branchSales.get(saleList.get(0)) + fileSale;
				Long commodityAmount = commoditySales.get(saleList.get(1)) + fileSale;
				//合計金額の桁が10桁を超えたときのエラー処理
				if(brunchAmount >= 10000000000L || commodityAmount >= 10000000000L){
					System.out.println(AMOUNT_DIGIT_OVER);
					return;
				}
				branchSales.put(saleList.get(0), brunchAmount);
				commoditySales.put(saleList.get(1), commodityAmount);
			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if(br != null) {
					try {
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}
		// 支店別集計ファイル書き込み処理、new商品別ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		//new商品別ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT,  commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 定義ファイルが存在しないエラー処理
	 * @param 定義ファイルのフォーマットが不正のエラー処理
	 * @param コードと対応する名前を保持するMap
	 * @param コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, String list, Map<String, String> namesMap, Map<String, Long> salesMap, String regex) {
		BufferedReader br = null;
		try {
			File file = new File(path, fileName);
			if(!file.exists()) {
				System.out.println(list + NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				String[] items = line.split(",");
				if(items.length != 2 || !items[0].matches(regex)) {
					System.out.println(list + FILE_INVALID_FORMAT);
					return false;
				}
				namesMap.put(items[0], items[1]);
				salesMap.put(items[0], (long)0);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param コードと対応する名前を保持するMap
	 * @param コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> namesMap, Map<String, Long> salesMap) {

		BufferedWriter bw = null;

		try {
			//書き込み先の指定
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			//集計ファイルへの書き込み
			for(String key : salesMap.keySet()) {
				bw.write(key + "," + namesMap.get(key) + "," + salesMap.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}
