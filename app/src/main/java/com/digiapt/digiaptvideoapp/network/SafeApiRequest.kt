package com.example.mvvm.data.network

import android.util.Log
import com.example.mvvm.util.ApiException
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

abstract class SafeApiRequest {

    suspend fun<T: Any> apiRequest(call: suspend () -> Response<T>) : T{
        val response = call.invoke()
        Log.d("ncm MyLog Response code",response.code().toString())
        if(response.isSuccessful){
            Log.d("ncm MyLog Response body",response.body().toString())
            return response.body()!!
        }else{
            val error = response.errorBody()?.string()

            val message = StringBuilder()
            error?.let{
                try{
                    message.append(JSONObject(it).getString("message"))
                }catch(e: JSONException){ }
                message.append("\n")
            }
            message.append("Error Code: ${response.code()}")
            Log.d("ncm MyLog Response body",message.toString())
            throw ApiException(message.toString())
        }
    }
}