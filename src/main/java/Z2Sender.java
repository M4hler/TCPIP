import java.net.*;

class Z2Sender
{
    private static final int datagramSize = 50;
    private static final int sleepTime = 500;
    private static final int maxPacket = 50;
    private InetAddress localHost;
    private int destinationPort;
    private DatagramSocket socket;
    private SenderThread sender;
    private ReceiverThread receiver;

    private byte[][] sended = new byte[512][5];
    private byte[][] received = new byte[512][5];
    private int sendedPackages = 0;
    private int receivedConfirmations = 0;

    public Z2Sender(int myPort, int destPort) throws Exception
    {
        localHost = InetAddress.getByName("127.0.0.1");
        destinationPort = destPort;
        socket = new DatagramSocket(myPort);
        sender = new SenderThread();
        receiver = new ReceiverThread();

        for(int i = 0; i < 512; i++)
        {
            received[i][0] = -1;
        }

        socket.setSoTimeout(10000); //Po 10 sekundach bez otrzymania pakietu socket.receive() przestanie blokowac resztę programu
    }

    class SenderThread extends Thread
    {
        public void run()
        {
            int x;
            try
            {
                for(int i = 0; (x = System.in.read()) >= 0 ; i++)
                {
                    Z2Packet p = new Z2Packet(4 + 1);
                    p.setIntAt(i,0);
                    p.data[4] = (byte) x;
                    DatagramPacket packet =
                            new DatagramPacket(p.data, p.data.length,
                                    localHost, destinationPort);
                    sended[i][0] = (byte)((i) & 0xFF);
                    sended[i][4] = (byte) x;

                    socket.send(packet);
                    sendedPackages++;
                    sleep(sleepTime);
                }
            }
            catch(Exception e)
            {
                System.out.println("Z2Sender.SenderThread.run: " + e);
            }
        }
    }

    class ReceiverThread extends Thread
    {
        public void run()
        {
            try
            {
                while(true)
                {
                    if (receivedConfirmations < sendedPackages || sendedPackages == 0)
                    {
                        byte[] data = new byte[datagramSize];
                        DatagramPacket packet = new DatagramPacket(data, datagramSize);

                        try
                        {
                            socket.receive(packet);
                        }
                        catch (SocketTimeoutException e)
                        {
                            for (int i = 0; i < sendedPackages; i++)
                            {
                                if (received[i][0] == -1)
                                {
                                    Z2Packet p = new Z2Packet(4+1);
                                    for(int j = 1; j <= 4; j++)
                                    {
                                        p.data[j]= sended[i][j];
                                    }
                                    p.setIntAt(i,0);
                                    packet = new DatagramPacket(p.data, p.data.length, localHost, destinationPort);
                                    socket.send(packet);
                                }
                            }
                        }

                        Z2Packet p = new Z2Packet(packet.getData());

                        int idx = p.getIntAt(0);
                        if (received[idx][0] == -1)
                        {
                            received[idx][0] = (byte)((idx) & 0xFF);
                            for (int i = 1; i <= 4; i++)
                            {
                                received[idx][i] = p.data[i];
                            }
                            receivedConfirmations++;
                        }

                        //Wysyłanie duplikatow pakietów, bo nie dostaliśmy ich wcześniejszych potwierdzeń
                        for (int i = 0; i <= idx; i++)
                        {
                            if (received[i][0] == -1)
                            {
                                p = new Z2Packet(4+1);
                                for (int j = 1; j <= 4; j++)
                                {
                                    p.data[j]= sended[i][j];
                                }
                                p.setIntAt(i,0);
                                packet = new DatagramPacket(p.data, p.data.length, localHost, destinationPort);
                                socket.send(packet);
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Otrzymano wszystkie potwierdzenia");
                        break;
                    }
                }
            }
            catch(Exception e)
            {
                System.out.println("Z2Sender.ReceiverThread.run: "+e);
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        Z2Sender sender=new Z2Sender( Integer.parseInt(args[0]), Integer.parseInt(args[1]));

        sender.sender.start();
        sender.receiver.start();
    }
}