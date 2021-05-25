package com.splunk.android.sample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.splunk.android.sample.databinding.FragmentFirstBinding;
import com.splunk.rum.SplunkRum;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.opentelemetry.api.trace.Span;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.apache.http.conn.ssl.SSLSocketFactory.SSL;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private OkHttpClient okHttpClient;
    private ExecutorService backgrounder = Executors.newSingleThreadExecutor();

    public FirstFragment() {
        okHttpClient = buildOkHttpClient();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment));

        binding.crash.setOnClickListener(v -> {
            throw new IllegalStateException("Crashing due to a bug!");
        });


        binding.httpMe.setOnClickListener(v -> {
            //this is NOT how you're supposed to do http/UI updates in Android.
            // TODO: fix this to use LiveData to update the UI
            Future<String> result = backgrounder.submit(() -> {
                Call call = okHttpClient.newCall(new Request.Builder().url("https://ssidhu.o11ystore.com/").get().build());
                try (Response r = call.execute()) {
                    int responseCode = r.code();
                    System.out.println("responseCode = " + responseCode);
                    return "" + responseCode;
                } catch (IOException e) {
                    //todo SplunkRum.noticeError(...)
                    Span.current().setAttribute("error", true);
                    Span.current().setAttribute("exception.kind", e.getClass().getSimpleName());

                    e.printStackTrace();
                    return "error!";
                }
            });
            try {
                binding.httpResult.setText(result.get(400, TimeUnit.MILLISECONDS));
            } catch (ExecutionException | InterruptedException e) {
                binding.httpResult.setText(e.getMessage());
                e.printStackTrace();
            } catch (TimeoutException e) {
                binding.httpResult.setText("timeout");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @NonNull
    private OkHttpClient buildOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(SplunkRum.getInstance().createOkHttpRumInterceptor());
        try {
            // NOTE: This is really bad and dangerous. Don't ever do this in the real world.
            // it's only necessary because the demo endpoint uses a self-signed SSL cert.
            SSLContext sslContext = SSLContext.getInstance(SSL);
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return builder
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(new AllowAllHostnameVerifier())
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    private static final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                               String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                               String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };

}