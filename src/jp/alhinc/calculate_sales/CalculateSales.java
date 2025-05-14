package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_CONTINUOUS = "売上ファイル名が連番になっていません";
	private static final String AMOUNT_DIGIT_OVER = "合計⾦額が10桁を超えました";
	private static final String BURANCHCODE_INVALID_FORMAT = "の支店コードが不正です";
	private static final String SALEFILE_INVALID_FORMAT ="のフォーマットが不正です";
	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length; i++) {
			String fileName = files[i].getName();
			if(fileName.matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}
		//ファイルが連番ではないときのエラー処理
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
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
				//売り上げファイルが2行じゃないときのエラー処理
				if(saleList.size() != 2) {
					System.out.println(rcdFiles.get(i).getName() + SALEFILE_INVALID_FORMAT);
					return;
				}
				//売り上げファイルの支店コードが支店定義ファイルにないときのエラー処理
				if(!branchNames.containsKey(saleList.get(0))) {
				    System.out.println(rcdFiles.get(i).getName() + BURANCHCODE_INVALID_FORMAT);
				    return;
				}
				long fileSale = Long.parseLong(saleList.get(1));
				Long saleAmount = branchSales.get(saleList.get(0)) + fileSale;
				//合計金額の桁が10桁を超えたときのエラー処理
				if(saleAmount >= 10000000000L){
					System.out.println(AMOUNT_DIGIT_OVER);
					return;
				}
				branchSales.put(saleList.get(0), saleAmount);
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
		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			if(!file.exists()) {
			   System.out.println(FILE_NOT_EXIST);
			   return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			//支店定義ファイルを読み込み、コードと支店名に分割する処理
			while((line = br.readLine()) != null) {
				String[] items = line.split(",");
				if((items.length != 2)||(!items[0].matches("^[0-9]{3}$"))) {
			    	System.out.println(FILE_INVALID_FORMAT);
			    	return false;
			    }
				branchNames.put(items[0], items[1]);
			    branchSales.put(items[0], (long)0);
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
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {

		BufferedWriter bw = null;

		try {
			//書き込み先の指定
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);
			//すべての支店を書き込むまで繰り返し
			for(String key : branchNames.keySet()) {
				//支店の書き込み
				bw.write(key);
				bw.write(",");
				bw.write(branchNames.get(key));
				bw.write(",");
				String longString = String.valueOf(branchSales.get(key));
				bw.write(longString);
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
