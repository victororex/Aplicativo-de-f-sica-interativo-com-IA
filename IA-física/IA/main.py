from ia.ai_service import responder_texto, responder_com_imagem


def iniciar_chat():
    print("IA Física iniciada.")
    print("1 - Perguntar por texto")
    print("2 - Perguntar com imagem")
    print("0 - Sair")

    while True:
        opcao = input("\nEscolha uma opção: ")

        if opcao == "0":
            print("Encerrando IA Física.")
            break

        elif opcao == "1":
            pergunta = input("Digite sua pergunta de Física: ")
            resposta = responder_texto(pergunta)
            print("\nResposta da IA:\n")
            print(resposta)

        elif opcao == "2":
            pergunta = input("Digite sua pergunta sobre a imagem: ")
            caminho = input("Digite o caminho da imagem: ")
            resposta = responder_com_imagem(pergunta, caminho)
            print("\nResposta da IA:\n")
            print(resposta)

        else:
            print("Opção inválida.")


if __name__ == "__main__":
    iniciar_chat()