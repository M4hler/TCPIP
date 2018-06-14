import java.net.*;

public class Z2Receiver
{
    static final int datagramSize = 50;
    InetAddress localHost;
    int destinationPort;
    DatagramSocket socket;

    ReceiverThread receiver;

    byte[][] sended = new byte[512][5];
    byte[][] received = new byte[512][5];
    int lastPrinted = 0;
    boolean gap = false;

    public Z2Receiver(int myPort, int destPort) throws Exception
    {
        localHost=InetAddress.getByName("127.0.0.1");
        destinationPort=destPort;
        socket=new DatagramSocket(myPort);


        for (int i = 0; i < 512; i++)
        {
            received[i][0] = -1;
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
                    byte[] data = new byte[datagramSize];
                    DatagramPacket packet = new DatagramPacket(data, datagramSize);
                    socket.receive(packet);
                    Z2Packet p = new Z2Packet(packet.getData());

                    int idx = p.getIntAt(0);
                    if (received[idx][0] == idx)
                    {
                        packet.setPort(destinationPort);
                        socket.send(packet);
                    }
                    else
                    {
                        received[idx][0] = (byte)((idx) & 0xFF);
                        for (int i = 1; i <= 4; i++)
                        {
                            received[idx][i] = p.data[i];
                        }

                        if (p.getIntAt(0) == 0)
                        {
                            lastPrinted = 0;
                            int i = 0;
                            System.out.print("R: ");

                            while (received[i][0] != -1)
                            {
                                System.out.print((char) received[i][4] + " ");
                                lastPrinted = i;
                                i++;
                            }
                            System.out.println("");
                        }
                        else
                        {
                            for (int i = idx; i >= lastPrinted; i--)
                            {
                                if (received[i][0] == -1)
                                {
                                    gap = true;
                                    break;
                                }
                                else
                                {
                                    gap = false;
                                }
                            }
                            if (gap == false)
                            {
                                int i = idx;
                                System.out.print("R: ");
                                while (received[i][0] != -1)
                                {
                                    System.out.print((char) received[i][4] + " ");
                                    lastPrinted = i;
                                    i++;
                                }
                                System.out.println("");
                            }
                            else
                            {
 /*                               for (int i = 0; i <= idx; i++)
                                {
                                    if(received[i][0] != i)
                                    {
                                        //System.out.print(i + " ");
                                    }
                                } */

                                gap = false;
                            }
                        }

                        packet.setPort(destinationPort); //potwierdzenie
                        socket.send(packet);
                    }
                }
            }
            catch(Exception e)
            {
                System.out.println("Z2Receiver.ReceiverThread.run: "+e);
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        Z2Receiver receiver=new Z2Receiver( Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        receiver.receiver.start();
    }
}