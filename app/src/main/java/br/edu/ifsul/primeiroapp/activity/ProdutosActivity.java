package br.edu.ifsul.primeiroapp.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.sql.CommonDataSource;

import br.edu.ifsul.primeiroapp.R;
import br.edu.ifsul.primeiroapp.adapter.ProdutosAdapter;
import br.edu.ifsul.primeiroapp.barcode.BarcodeCaptureActivity;
import br.edu.ifsul.primeiroapp.model.Item;
import br.edu.ifsul.primeiroapp.model.Produto;
import br.edu.ifsul.primeiroapp.setup.AppSetup;

public class ProdutosActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int RC_BARCODE_CAPTURE = 1;
    private ListView lvProdutos;
    private List<Produto> produtos;
    private static final String TAG = "produtosActivity";
    private DatabaseReference myRef = null;
    private DrawerLayout drawer;
    public static List<String> keysProdutos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //configuracoes da navegation view
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        lvProdutos = findViewById(R.id.lvProdutos);
        lvProdutos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isNoItens(position)) {
                    Toast.makeText(ProdutosActivity.this, "Produto no carrinho", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Objeto clicaco:" + AppSetup.produtos.get(position));
                    Intent intent = new Intent(ProdutosActivity.this, ProdutoDetalheActivity.class);
                    intent.putExtra("position", position);
                    startActivity(intent);
                }
            }
        });
        produtos = new ArrayList<>();

        myRef = AppSetup.getInstance().child("produtos");
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Log.d(TAG, "Value is: \n" + dataSnapshot);
                if (dataSnapshot.getValue() != null) {
                    Log.d(TAG, "dados do firebase " + dataSnapshot.getValue());

                    GenericTypeIndicator<Map<String, Produto>> type = new GenericTypeIndicator<Map<String, Produto>>() {
                    };

                    keysProdutos = new ArrayList<>(dataSnapshot.getValue(type).keySet());
                    AppSetup.produtos = new ArrayList<>(dataSnapshot.getValue(type).values());

                    for (int i = 0; i < AppSetup.produtos.size(); i++) {
                        AppSetup.produtos.get(i).setKey(keysProdutos.get(i));

                    }

                    Collections.sort(AppSetup.produtos, new Comparator<Produto>() {
                        @Override
                        public int compare(Produto o1, Produto o2) {

                            if (o1.getNome().compareToIgnoreCase(o2.getNome()) < 0)
                                return -1;
                            else if (o1.getNome().compareToIgnoreCase(o2.getNome()) > 0)
                                return +1;
                            else
                                return 0;
                        }
                    });

                    atualizarView();
                } else {
                    Toast.makeText(ProdutosActivity.this, "Não há dados cadastrados.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }


        });
    }



    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_carrinho: {
                // Handle the camera action
                if (AppSetup.itens.isEmpty()) {
                    Toast.makeText(this, "Cesta Está Vazia !", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(this, CestaActivity.class));
                }
                break;
            }
            case R.id.nav_clientes: {
                startActivity(new Intent(this, ClientesActivity.class));
                break;
            }
            case R.id.nav_produtoAdm: {
                startActivity(new Intent(this, ProdutoAdminActivity.class));
                break;
            }
            case R.id.nav_clienteAdm: {
                startActivity(new Intent(this, ClienteAdminActivity.class));
                break;
            }
            case R.id.nav_sobre: {
                startActivity(new Intent(this, SobreActivity.class));
                break;
            }
            case R.id.nav_sair: {
                if (!AppSetup.itens.isEmpty()) {
                    alertDialogSimNao("ATEN??O", "Se voc? sair do aplicativo, os itens do carrinho ser?o perdidos !");
                } else {
                    closeDrawer();
                    finish();
                }
                break;
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_barcode: {

                Intent intent = new Intent(this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true); //true liga a funcionalidade autofoco
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false); //true liga a lanterna (fash)
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                break;
            }
        }

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    //localiza o produto na lista (ou n?o)
                    boolean flag = true;
                    int position = 0;
                    for (Produto produto : AppSetup.produtos) {
                        if (String.valueOf(produto.getCodigoDeBarras()).equals(barcode.displayValue)) {
                            flag = false;
                            Intent intent = new Intent(ProdutosActivity.this, ProdutoDetalheActivity.class);
                            intent.putExtra("position", position);
                            startActivity(intent);
                            break;
                        }
                        position++;
                    }
                    if (flag) {
                        Toast.makeText(this, "Produto não cadastrado.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.barcode_failure, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                Toast.makeText(this, String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)), Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_produtos, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menuitem_pesquisar).getActionView();
        searchView.setQueryHint("Digite o nome do produto");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //uma lista para nova camada de modelo da RecyclerViewif(dataSnapshot.getValue() != null){
                List<Produto> produtosFilter = new ArrayList<>();

                //um for-each na camada de modelo atual
                for (Produto produto : produtos) {
                    //se o nome do produto comeca com o nome digitado
                    if (produto.getNome().contains(newText)) {
                        //adiciona o produto na nova lista
                        produtosFilter.add(produto);
                    }

                }

                //coloca a nova lista como fonte de dados do novo adapter de RecyclerView
                //(Context, fonte de dados)
                lvProdutos.setAdapter(new ProdutosAdapter(ProdutosActivity.this, produtosFilter));

                return true;
            }
        });

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < AppSetup.itens.size() ; i++) {
            atualizaEstoque(i);
        }

        //reset do setup da app
        AppSetup.itens = new ArrayList<>();
        AppSetup.produtos = new ArrayList<>();
        AppSetup.cliente = null;
    }

    private boolean isNoItens(int position) {
        for (Item item : AppSetup.itens) {
            if (item.getProduto().getKey().equals(AppSetup.produtos.get(position).getKey())) {
                return true;
            }
        }
        return false;
    }

    private void atualizarView() {
        lvProdutos.setAdapter(new ProdutosAdapter(ProdutosActivity.this, AppSetup.produtos));
    }

    private void closeDrawer() {
        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onBackPressed() {
        drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!AppSetup.itens.isEmpty()) {
                alertDialogSimNao("ATEN??O", "Se voc? sair do aplicativo, os itens do carrinho ser?o perdidos !");

            } else {
                finish();
            }
        }
    }

    private void alertDialogSimNao(String titulo, String mensagem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //add the title and text
        builder.setTitle(titulo);
        builder.setMessage(mensagem);
        //add the buttons
        builder.setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!AppSetup.itens.isEmpty()) {
                    for (int i = 0; i < AppSetup.itens.size() ; i++) {
                        atualizaEstoque(i);
                    }
                }
                finish();
            }
        });
        builder.setNegativeButton(R.string.nao, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ProdutosActivity.this, "Opera??o cancelada.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void atualizaEstoque(final int position) {
        //atualiza estoque no Firebase (Essa atualização é temporária, ao efetivar o pedido isso deverá ser validado.)
        final DatabaseReference myRef = AppSetup.getInstance().child("produtos").child(AppSetup.itens.get(position).getProduto().getKey()).child("quantidade");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //busca a posição de estoque atual
                long quantidade = (long) dataSnapshot.getValue();
                //atualiza o estoque
                myRef.setValue(AppSetup.itens.get(position).getQuantidade() + quantidade);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



}
