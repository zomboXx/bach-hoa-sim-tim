package vn.simtim.api.sale.application;

import java.nio.charset.StandardCharsets;

/** NAPAS247 account transfer, EMV merchant-presented TLV with CRC-16/CCITT-FALSE. */
public final class VietQr {
    private VietQr() {}
    public static String encode(String bin, String account, long amount, String reference) {
        if (!bin.matches("[0-9]{6}") || !account.matches("[A-Za-z0-9]{1,19}")
                || amount<=0 || amount>9999999999999L || !reference.matches("[A-Za-z0-9 ]{1,25}")) {
            throw new IllegalArgumentException("Thông tin QR không hợp lệ");
        }
        String beneficiary=tlv("00",bin)+tlv("01",account);
        String provider=tlv("00","A000000727")+tlv("01",beneficiary)+tlv("02","QRIBFTTA");
        String payload=tlv("00","01")+tlv("01","12")+tlv("38",provider)+tlv("53","704")
                +tlv("54",Long.toString(amount))+tlv("58","VN")+tlv("62",tlv("08",reference))+"6304";
        int crc=0xffff;
        for (byte value:payload.getBytes(StandardCharsets.UTF_8)) {
            crc^=(value & 0xff)<<8;
            for (int bit=0;bit<8;bit++) crc=((crc & 0x8000)!=0 ? (crc<<1)^0x1021 : crc<<1)&0xffff;
        }
        return payload+String.format("%04X",crc);
    }
    private static String tlv(String tag,String value) { return tag+String.format("%02d",value.length())+value; }
}
