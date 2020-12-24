package com.digiapt.digiaptvideoapp.util

class Constants {

    companion object{


        const val BASE_URL_SERVER = "http://5975cc5f.ngrok.io/v1/"

        //---------------------------------------------------------------------------------//

        const val BASE_URL_MOCKY = "http://www.mocky.io/v2/"
        const val HOME_PAGE_MOCKY = "5e466bad330000894902615a"
        const val HOME_PAGE_HIGHLIGHT_VIDEO_MOCKY = "5e32d08f3200005f0094d161"

        //---------------------------------------------------------------------------------//

        const val BASE_URL_VDOCIPHER = "https://dev.vdocipher.com/api/"

        const val DIGIAPT_SECRET_KEY = "Apisecret 0b4EkWoHLsCxp7VK5gicDdDIUXsuON7cL8vauZ3Rqat3AA9P6Ga2ZVYZwnh4lBgG"
        const val MY_SECRET_KEY = "Apisecret sO7EpcsqYKiOdt2r7jB3hnLSFejeGXVYU93mSviwCEhmSvCXVRcd5t2nXoDVHqI2"
        const val EXPLORE_KEY = "Apisecret lTa2wLoisb6uRuTHaAT4Wy559aQcuXEel7qlYG1C6eSNw10OXaOrg1Dw6hcQGFFs"

        const val GET_VIDEO_OFFLINE_OTP_POST_VALUE = "{\n" +
                " \"licenseRules\": json_serialize({\n" +
                "   \"canPersist\": true,\n" +
                "   \"rentalDuration\": 15 * 24 * 3600,\n" +
                " })\n" +
                "}"

        //---------------------------------------------------------------------------------//

        const val BASE_URL = BASE_URL_MOCKY

        const val HOME_PAGE = HOME_PAGE_MOCKY
        const val HOME_PAGE_HIGHLIGHT_VIDEO = HOME_PAGE_HIGHLIGHT_VIDEO_MOCKY

        const val VDOCIPHER_AUTHORIZATION_KEY = "Authorization"
        const val VDOCIPHER_AUTHORIZATION_VALUE = EXPLORE_KEY
    }
}